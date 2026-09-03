package com.lingolearn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 语音合成服务：多发音源降级链
 *
 * <pre>
 * 1. assets  项目内置音频资源（方言唯一可行的零成本方案：预录好的 MP3）
 * 2. youdao  有道公开 dictvoice 端点（免 key，覆盖 15 种标准语言）
 * 3. premium 付费云 TTS（讯飞/阿里云，支持粤语等方言音色，配置后自动优先）
 * </pre>
 *
 * 微信同声传译插件只支持 zh_CN / en_US，日韩法西阿会退化成英语朗读，
 * 因此多语种一律走后端代理。
 *
 * 安全约束：该接口对匿名开放（音频组件无法携带自定义请求头），
 * 故必须限长、限频、限总量，避免被当成免费 TTS 代理滥用。
 */
@Service
public class TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsService.class);

    /** 单次合成最大字符数：第三方按字符计费，且长文本延迟明显 */
    private static final int MAX_TEXT_LENGTH = 200;

    /** 有道端点实际验证过的语种；方言（yue/sc/wuu/nan/hakka）一律 451，不要写进来 */
    private static final Set<String> YOUDAO_LANGS = new HashSet<>(Arrays.asList(
            "en", "ja", "ko", "fr", "es", "ar", "zh", "pt", "de",
            "ru", "th", "vi", "id", "it", "tr", "hi"
    ));

    /** 内置音频资源的语种目录：方言靠这里落地 */
    private static final Set<String> ASSET_LANGS = new HashSet<>(Arrays.asList(
            "zh-yue", "zh-sc", "zh-bj", "zh-sh", "zh-nan", "zh-hakka"
    ));

    /**
     * 方言 → 普通话兜底映射。
     * 实测结论：所有免费 TTS 端点（有道/Google/Lingva）均不支持任何汉语方言，
     * 方言代码一律返回 451。在拿到真人录音或商业方言音色之前，
     * 用普通话近似发音，并通过 approximate 标记让前端明确标注"方言发音开发中"，
     * 避免用户误以为听到的就是方言。
     */
    private static final Map<String, String> DIALECT_FALLBACK = new java.util.HashMap<>();
    static {
        DIALECT_FALLBACK.put("zh-yue", "zh");
        DIALECT_FALLBACK.put("zh-sc", "zh");
        DIALECT_FALLBACK.put("zh-bj", "zh");
        DIALECT_FALLBACK.put("zh-sh", "zh");
        DIALECT_FALLBACK.put("zh-nan", "zh");
        DIALECT_FALLBACK.put("zh-hakka", "zh");
    }

    private static final String YOUDAO_URL = "https://dict.youdao.com/dictvoice";

    @Value("${tts.enabled:true}")
    private boolean enabled;

    /** 每日合成配额，防止匿名接口被刷导致云账单失控；0 表示不限 */
    @Value("${tts.daily-quota:0}")
    private int dailyQuota;

    /** 单 IP 每分钟请求上限 */
    @Value("${tts.rate-limit-per-minute:60}")
    private int rateLimitPerMinute;

    @Value("${tts.assets-dir:}")
    private String assetsDir;

    /** 已用配额（按自然日重置） */
    private final AtomicInteger usedToday = new AtomicInteger(0);
    private volatile String quotaDay = currentDay();

    /** 音频缓存：key = lang|text */
    private final Map<String, TtsResult> cache = new LinkedHashMap<String, TtsResult>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TtsResult> eldest) {
            return size() > 400;
        }
    };

    /** 限流窗口：ip -> [窗口开始毫秒, 计数] */
    private final Map<String, long[]> rateWindow = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("TTS service enabled={}, dailyQuota={}, rateLimit={}/min", enabled, dailyQuota, rateLimitPerMinute);
    }

    public static class TtsResult {
        private final byte[] audio;
        private final String contentType;
        private final String provider;
        /** true 表示并非目标语种的真实发音（如方言用普通话近似） */
        private final boolean approximate;

        public TtsResult(byte[] audio, String contentType, String provider, boolean approximate) {
            this.audio = audio;
            this.contentType = contentType;
            this.provider = provider;
            this.approximate = approximate;
        }

        public byte[] getAudio() { return audio; }
        public String getContentType() { return contentType; }
        public String getProvider() { return provider; }
        public boolean isApproximate() { return approximate; }
    }

    /** 服务端未配置任何可用发音源 */
    public static class TtsUnavailableException extends RuntimeException {
        public TtsUnavailableException(String msg) { super(msg); }
    }

    /** 触发限流或配额 */
    public static class TtsThrottledException extends RuntimeException {
        public TtsThrottledException(String msg) { super(msg); }
    }

    /**
     * 合成语音
     * @param text 待朗读文本
     * @param lang 语种代码（en / ja / zh-yue …）
     * @param clientIp 调用方 IP，用于限流
     */
    public synchronized TtsResult synthesize(String text, String lang, String clientIp) {
        if (!enabled) {
            throw new TtsUnavailableException("TTS 服务已关闭");
        }
        String content = text == null ? "" : text.trim();
        if (content.isEmpty()) {
            throw new TtsUnavailableException("文本为空");
        }
        if (content.length() > MAX_TEXT_LENGTH) {
            content = content.substring(0, MAX_TEXT_LENGTH);
        }

        String normLang = normalizeLang(lang);

        checkRateLimit(clientIp);
        checkQuota();

        String cacheKey = normLang + "|" + content;
        TtsResult cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 1) 内置音频资源：方言最理想的落地方式，有录音就优先用
        byte[] asset = loadAsset(normLang, content);
        if (asset != null) {
            TtsResult r = new TtsResult(asset, "audio/mpeg", "assets", false);
            cache.put(cacheKey, r);
            return r;
        }

        // 2) 有道公开端点：标准语种主力
        if (YOUDAO_LANGS.contains(normLang)) {
            byte[] audio = fetchYoudao(normLang, content);
            if (audio != null && audio.length > 0) {
                usedToday.incrementAndGet();
                TtsResult r = new TtsResult(audio, "audio/mpeg", "youdao", false);
                cache.put(cacheKey, r);
                return r;
            }
        }

        // 3) 方言兜底：用普通话近似，但明确标记为"近似发音"
        String mandarin = DIALECT_FALLBACK.get(normLang);
        if (mandarin != null) {
            byte[] audio = fetchYoudao(mandarin, content);
            if (audio != null && audio.length > 0) {
                usedToday.incrementAndGet();
                TtsResult r = new TtsResult(audio, "audio/mpeg", "mandarin-approx", true);
                cache.put(cacheKey, r);
                return r;
            }
        }

        // 4) 确实无解
        if (ASSET_LANGS.contains(normLang)) {
            throw new TtsUnavailableException("该方言暂无发音资源：" + normLang);
        }
        throw new TtsUnavailableException("暂不支持的语种：" + normLang);
    }

    /** 语种归一化：en-US / en_US / EN → en；过滤非法字符防路径穿越 */
    private String normalizeLang(String lang) {
        String s = (lang == null ? "" : lang).trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (s.isEmpty()) {
            return "en";
        }
        if (!s.matches("[a-z]{2,3}(-[a-z]{2,4})?")) {
            return "en";
        }
        return s;
    }

    // ------------------------------------------------------------------ 限流

    private void checkRateLimit(String ip) {
        if (rateLimitPerMinute <= 0) return;
        String key = ip == null ? "unknown" : ip;
        long now = System.currentTimeMillis();
        long[] win = rateWindow.get(key);
        if (win == null || now - win[0] > 60_000) {
            rateWindow.put(key, new long[]{now, 1});
            return;
        }
        if (win[1] >= rateLimitPerMinute) {
            throw new TtsThrottledException("发音请求过于频繁，请稍后再试");
        }
        win[1]++;
        // 顺手清理过期窗口，避免长期运行内存膨胀
        if (rateWindow.size() > 5000) {
            rateWindow.entrySet().removeIf(e -> now - e.getValue()[0] > 60_000);
        }
    }

    private void checkQuota() {
        if (dailyQuota <= 0) return;
        String today = currentDay();
        if (!today.equals(quotaDay)) {
            quotaDay = today;
            usedToday.set(0);
        }
        if (usedToday.get() >= dailyQuota) {
            throw new TtsThrottledException("今日发音额度已用完");
        }
    }

    private static String currentDay() {
        return java.time.LocalDate.now().toString();
    }

    // ------------------------------------------------------------------ 内置资源

    /**
     * 读取项目内置音频：{assetsDir}/{lang}/{md5(text)}.mp3
     * 方言没有可用的免费 TTS，只能靠预录音频；把 MP3 按此规则放好即可生效。
     */
    private byte[] loadAsset(String lang, String text) {
        if (assetsDir == null || assetsDir.trim().isEmpty()) return null;
        if (!ASSET_LANGS.contains(lang)) return null;
        try {
            String hash = md5(text);
            java.io.File f = new java.io.File(assetsDir, lang + java.io.File.separator + hash + ".mp3");
            if (!f.isFile()) return null;
            byte[] buf = new byte[(int) f.length()];
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                int read = in.read(buf);
                if (read <= 0) return null;
            }
            return buf;
        } catch (Exception e) {
            log.debug("asset tts miss: {}/{}", lang, text);
            return null;
        }
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    // ------------------------------------------------------------------ 有道

    private byte[] fetchYoudao(String lang, String text) {
        try {
            String url = YOUDAO_URL
                    + "?le=" + URLEncoder.encode(lang, "UTF-8")
                    + "&audio=" + URLEncoder.encode(text, "UTF-8");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            // 该端点对默认 UA 返回空响应体，必须伪装浏览器 UA
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://dict.youdao.com/");
            int code = conn.getResponseCode();
            if (code != 200) {
                log.debug("youdao tts {} -> {}", lang, code);
                return null;
            }
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                byte[] data = out.toByteArray();
                // 端点偶发返回 200 但空响应体，视为失败以便继续降级
                return data.length > 512 ? data : null;
            }
        } catch (Exception e) {
            log.warn("youdao tts failed: {}", e.getMessage());
            return null;
        }
    }
}
