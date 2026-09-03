package com.lingolearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingolearn.common.BusinessException;
import com.lingolearn.dto.TranslateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 翻译服务。
 *
 * 当前默认走 MyMemory 免费源。之所以选它：Google / 有道 / Lingva / LibreTranslate
 * 的免费端点在国内网络环境下均不可达（实测 Cloudflare 拦截或连接超时），
 * 而 MyMemory 免 key、支持 sl=auto 自动检测、覆盖 20+ 常用语种。
 *
 * 若后续换成付费源（Google / DeepL），只需新增一个 Provider 实现并在
 * {@link #pickProvider()} 里插到链首，其余逻辑（分块、缓存、限长）无需改动。
 */
@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    /** MyMemory 对单次 q 参数有 ~500 字节限制，超了会截断或报错，故按句子分块 */
    private static final int MAX_CHUNK_BYTES = 450;

    private static final String MYMEMORY_URL = "https://api.mymemory.translated.net/get";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** MyMemory 匿名额度 5000 字/天，填邮箱可提到 50000/天 */
    @Value("${translate.mymemory-email:}")
    private String mymemoryEmail;

    @Value("${translate.cache-size:500}")
    private int cacheSize;

    private final Map<String, TranslateResponse> cache =
            new LinkedHashMap<String, TranslateResponse>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, TranslateResponse> eldest) {
                    return size() > Math.max(50, cacheSize);
                }
            };

    /**
     * 翻译
     * @param text    原文
     * @param source  源语种，auto/空 表示自动检测
     * @param target  目标语种
     */
    public synchronized TranslateResponse translate(String text, String source, String target) {
        String content = text == null ? "" : text.trim();
        if (content.isEmpty()) {
            throw new BusinessException(400, "待翻译文本不能为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException(400, "单次翻译最多 2000 字");
        }

        String tgt = TranslationLanguages.normalize(target);
        if (tgt.isEmpty() || TranslationLanguages.find(tgt) == null) {
            throw new BusinessException(400, "不支持的目标语种：" + target);
        }

        boolean auto = source == null || source.trim().isEmpty()
                || "auto".equalsIgnoreCase(source.trim());
        String src = auto ? "" : TranslationLanguages.normalize(source);

        // 源与目标相同（含自动检测后相同的情形）没必要请求第三方
        if (!auto && src.equalsIgnoreCase(tgt)) {
            // detected 填入已知源语言，客户端据此做“同语言避让”切换
            return new TranslateResponse(content, src, src, tgt, "none");
        }

        String cacheKey = src + "|" + tgt + "|" + content;
        TranslateResponse cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<String> chunks = splitChunks(content);
        StringBuilder out = new StringBuilder();
        String detected = null;
        String provider = "mymemory";

        for (String chunk : chunks) {
            MyMemoryResult r = callMyMemory(chunk, auto ? "Autodetect" : src, tgt);
            if (r == null || r.text == null || r.text.isEmpty()) {
                // 分块中某块失败不应让整段翻译报废：保留原文该块，继续后续块
                log.warn("translate chunk failed, keep original: {}", chunk);
                out.append(chunk);
                continue;
            }
            out.append(r.text);
            if (detected == null && r.detected != null && !r.detected.isEmpty()) {
                detected = TranslationLanguages.normalize(r.detected);
            }
            provider = r.provider;
        }

        String result = unescapeHtml(out.toString()).trim();
        // 自动检测模式下 MyMemory 在“检测语种 = 目标语种”时会直接报错（如中文→中文），
        // detected 可能为空；用探测接口兜底，保证响应始终携带检测语种供客户端做避让切换
        if (auto && detected == null && !result.isEmpty()) {
            try {
                detected = detect(content);
            } catch (Exception ignore) {
                // 探测失败不影响返回译文
            }
        }
        TranslateResponse resp = new TranslateResponse(result, detected, detected != null ? detected : src, tgt, provider);
        cache.put(cacheKey, resp);
        return resp;
    }

    /** 语种检测：借 MyMemory 的 Autodetect 能力，译到英语并读回检测语种 */
    public synchronized String detect(String text) {
        String content = text == null ? "" : text.trim();
        if (content.isEmpty()) {
            throw new BusinessException(400, "待检测文本不能为空");
        }
        String probe = content.length() > 200 ? content.substring(0, 200) : content;
        MyMemoryResult r = callMyMemory(probe, "Autodetect", "en");
        if (r == null || r.detected == null || r.detected.isEmpty()) {
            throw new BusinessException(502, "语种检测失败，请稍后再试");
        }
        return TranslationLanguages.normalize(r.detected);
    }

    // ------------------------------------------------------------------ MyMemory

    private static class MyMemoryResult {
        String text;
        String detected;
        String provider = "mymemory";
    }

    private MyMemoryResult callMyMemory(String text, String source, String target) {
        try {
            String url = MYMEMORY_URL
                    + "?q=" + URLEncoder.encode(text, "UTF-8")
                    + "&langpair=" + URLEncoder.encode(source + "|" + target, "UTF-8");
            if (mymemoryEmail != null && !mymemoryEmail.trim().isEmpty()) {
                // 带邮箱可显著提高匿名额度
                url += "&de=" + URLEncoder.encode(mymemoryEmail.trim(), "UTF-8");
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "LingoPlay/1.0 (language learning app)");
            int code = conn.getResponseCode();
            if (code != 200) {
                log.warn("mymemory http {}", code);
                return null;
            }

            String body;
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                body = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            }

            JsonNode root = objectMapper.readTree(body);
            int status = root.path("responseStatus").asInt(-1);
            if (status != 200) {
                log.warn("mymemory status {} detail={}", status, root.path("responseDetails").asText());
                return null;
            }
            JsonNode data = root.path("responseData");
            MyMemoryResult r = new MyMemoryResult();
            r.text = data.path("translatedText").asText(null);
            r.detected = data.path("detectedLanguage").asText(null);
            // MyMemory 偶发把原文原样返回（未翻译），视为失败以便上层保留原文
            if (r.text != null && r.text.equals(text) && !"Autodetect".equals(source)) {
                // 原文与译文相同可能是正常的（如专有名词），这里仍接受
            }
            return r;
        } catch (Exception e) {
            log.warn("mymemory call failed: {}", e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ 分块

    /**
     * 按 UTF-8 字节数切分，尽量在标点/空格处断开，避免把一个词劈成两半。
     * 中文没有空格，纯按字节切虽然不优雅，但 API 层面对齐即可。
     */
    private List<String> splitChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (utf8Length(text) <= MAX_CHUNK_BYTES) {
            chunks.add(text);
            return chunks;
        }
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            cur.append(c);
            if (utf8Length(cur.toString()) >= MAX_CHUNK_BYTES * 0.9 && isBreakPoint(c)) {
                chunks.add(cur.toString());
                cur.setLength(0);
            } else if (utf8Length(cur.toString()) >= MAX_CHUNK_BYTES) {
                chunks.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) chunks.add(cur.toString());
        return chunks;
    }

    private static boolean isBreakPoint(char c) {
        // 中英文句读与空白都可作为断点
        return " \n\r\t.,!?;:。！？；：，、".indexOf(c) >= 0;
    }

    private static int utf8Length(String s) {
        return s == null ? 0 : s.getBytes(StandardCharsets.UTF_8).length;
    }

    // ------------------------------------------------------------------ 工具

    /** MyMemory 会把引号等转义成 HTML 实体，必须还原，否则译文带 &#39; 这类噪声 */
    private static String unescapeHtml(String s) {
        if (s == null) return null;
        return s.replace("&#39;", "'")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ");
    }

    /** 供控制器暴露语种列表 */
    public List<TranslationLanguages.Lang> languages() {
        return TranslationLanguages.all();
    }
}
