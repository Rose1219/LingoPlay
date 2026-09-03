package com.lingolearn.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 翻译与界面语言共用的语种目录。
 *
 * code 统一使用 MyMemory 接受的标签（zh-CN / zh-TW 这类带地区的写法），
 * 界面显示名直接用各语种母语写法——面向国际用户时，用母语写语言名
 * 比写英文更友好（西班牙语用户更容易认出 "Español" 而不是 "Spanish"）。
 */
public final class TranslationLanguages {

    private TranslationLanguages() {}

    public static class Lang {
        public final String code;
        public final String nativeName;
        public final String englishName;
        public final String chineseName;
        public final String flag;

        public Lang(String code, String nativeName, String englishName, String chineseName, String flag) {
            this.code = code;
            this.nativeName = nativeName;
            this.englishName = englishName;
            this.chineseName = chineseName;
            this.flag = flag;
        }
    }

    private static final Lang[] LANGS = {
            new Lang("zh-CN", "简体中文", "Chinese (Simplified)", "简体中文", "🇨🇳"),
            new Lang("zh-TW", "繁體中文", "Chinese (Traditional)", "繁体中文", "🇹🇼"),
            new Lang("en", "English", "English", "英语", "🇬🇧"),
            new Lang("ja", "日本語", "Japanese", "日语", "🇯🇵"),
            new Lang("ko", "한국어", "Korean", "韩语", "🇰🇷"),
            new Lang("es", "Español", "Spanish", "西班牙语", "🇪🇸"),
            new Lang("fr", "Français", "French", "法语", "🇫🇷"),
            new Lang("ar", "العربية", "Arabic", "阿拉伯语", "🇸🇦"),
            new Lang("th", "ไทย", "Thai", "泰语", "🇹🇭"),
            new Lang("vi", "Tiếng Việt", "Vietnamese", "越南语", "🇻🇳"),
            new Lang("de", "Deutsch", "German", "德语", "🇩🇪"),
            new Lang("ru", "Русский", "Russian", "俄语", "🇷🇺"),
            new Lang("pt", "Português", "Portuguese", "葡萄牙语", "🇵🇹"),
            new Lang("it", "Italiano", "Italian", "意大利语", "🇮🇹"),
            new Lang("hi", "हिन्दी", "Hindi", "印地语", "🇮🇳"),
            new Lang("tr", "Türkçe", "Turkish", "土耳其语", "🇹🇷"),
            new Lang("id", "Bahasa Indonesia", "Indonesian", "印尼语", "🇮🇩"),
            new Lang("nl", "Nederlands", "Dutch", "荷兰语", "🇳🇱"),
            new Lang("pl", "Polski", "Polish", "波兰语", "🇵🇱"),
            new Lang("uk", "Українська", "Ukrainian", "乌克兰语", "🇺🇦"),
            new Lang("sv", "Svenska", "Swedish", "瑞典语", "🇸🇪"),
            new Lang("fa", "فارسی", "Persian", "波斯语", "🇮🇷"),
            new Lang("he", "עברית", "Hebrew", "希伯来语", "🇮🇱"),
            new Lang("bn", "বাংলা", "Bengali", "孟加拉语", "🇧🇩"),
            new Lang("sw", "Kiswahili", "Swahili", "斯瓦希里语", "🇰🇪"),
            new Lang("ms", "Bahasa Melayu", "Malay", "马来语", "🇲🇾"),
            new Lang("tl", "Filipino", "Filipino", "菲律宾语", "🇵🇭"),
            new Lang("ur", "اردو", "Urdu", "乌尔都语", "🇵🇰"),
            new Lang("ta", "தமிழ்", "Tamil", "泰米尔语", "🇱🇰"),
            new Lang("te", "తెలుగు", "Telugu", "泰卢固语", "🇮🇳"),
            new Lang("mr", "मराठी", "Marathi", "马拉地语", "🇮🇳"),
            new Lang("cs", "Čeština", "Czech", "捷克语", "🇨🇿"),
            new Lang("el", "Ελληνικά", "Greek", "希腊语", "🇬🇷"),
            new Lang("ro", "Română", "Romanian", "罗马尼亚语", "🇷🇴"),
            new Lang("hu", "Magyar", "Hungarian", "匈牙利语", "🇭🇺"),
            new Lang("da", "Dansk", "Danish", "丹麦语", "🇩🇰"),
            new Lang("fi", "Suomi", "Finnish", "芬兰语", "🇫🇮"),
            new Lang("no", "Norsk", "Norwegian", "挪威语", "🇳🇴"),
            new Lang("sk", "Slovenčina", "Slovak", "斯洛伐克语", "🇸🇰"),
            new Lang("bg", "Български", "Bulgarian", "保加利亚语", "🇧🇬"),
            new Lang("hr", "Hrvatski", "Croatian", "克罗地亚语", "🇭🇷"),
            new Lang("sr", "Српски", "Serbian", "塞尔维亚语", "🇷🇸"),
            new Lang("lt", "Lietuvių", "Lithuanian", "立陶宛语", "🇱🇹"),
            new Lang("lv", "Latviešu", "Latvian", "拉脱维亚语", "🇱🇻"),
            new Lang("et", "Eesti", "Estonian", "爱沙尼亚语", "🇪🇪"),
            new Lang("sl", "Slovenščina", "Slovenian", "斯洛文尼亚语", "🇸🇮"),
            new Lang("af", "Afrikaans", "Afrikaans", "南非荷兰语", "🇿🇦"),
            new Lang("sq", "Shqip", "Albanian", "阿尔巴尼亚语", "🇦🇱"),
            new Lang("am", "አማርኛ", "Amharic", "阿姆哈拉语", "🇪🇹"),
            new Lang("hy", "Հայերեն", "Armenian", "亚美尼亚语", "🇦🇲"),
            new Lang("az", "Azərbaycan", "Azerbaijani", "阿塞拜疆语", "🇦🇿"),
            new Lang("eu", "Euskara", "Basque", "巴斯克语", "🇪🇸"),
            new Lang("be", "Беларуская", "Belarusian", "白俄罗斯语", "🇧🇾"),
            new Lang("ca", "Català", "Catalan", "加泰罗尼亚语", "🇦🇩"),
            new Lang("ga", "Gaeilge", "Irish", "爱尔兰语", "🇮🇪"),
            new Lang("gl", "Galego", "Galician", "加利西亚语", "🇪🇸"),
            new Lang("ka", "ქართული", "Georgian", "格鲁吉亚语", "🇬🇪"),
            new Lang("gu", "ગુજરાતી", "Gujarati", "古吉拉特语", "🇮🇳"),
            new Lang("ht", "Kreyòl", "Haitian Creole", "海地克里奥尔语", "🇭🇹"),
            new Lang("is", "Íslenska", "Icelandic", "冰岛语", "🇮🇸"),
            new Lang("kk", "Қазақ", "Kazakh", "哈萨克语", "🇰🇿"),
            new Lang("km", "ខ្មែរ", "Khmer", "高棉语", "🇰🇭"),
            new Lang("kn", "ಕನ್ನಡ", "Kannada", "卡纳达语", "🇮🇳"),
            new Lang("lo", "ລາວ", "Lao", "老挝语", "🇱🇦"),
            new Lang("mk", "Македонски", "Macedonian", "马其顿语", "🇲🇰"),
            new Lang("ml", "മലയാളം", "Malayalam", "马拉雅拉姆语", "🇮🇳"),
            new Lang("mt", "Malti", "Maltese", "马耳他语", "🇲🇹"),
            new Lang("mn", "Монгол", "Mongolian", "蒙古语", "🇲🇳"),
            new Lang("my", "ဗမာ", "Burmese", "缅甸语", "🇲🇲"),
            new Lang("ne", "नेपाली", "Nepali", "尼泊尔语", "🇳🇵"),
            new Lang("pa", "ਪੰਜਾਬੀ", "Punjabi", "旁遮普语", "🇮🇳"),
            new Lang("si", "සිංහල", "Sinhala", "僧伽罗语", "🇱🇰"),
            new Lang("uz", "O'zbek", "Uzbek", "乌兹别克语", "🇺🇿"),
            new Lang("cy", "Cymraeg", "Welsh", "威尔士语", "🏴"),
            new Lang("yi", "ייִדיש", "Yiddish", "意第绪语", "🇮🇱"),
            new Lang("zu", "isiZulu", "Zulu", "祖鲁语", "🇿🇦")
    };

    private static final Map<String, Lang> INDEX = new LinkedHashMap<>();
    private static final List<Lang> ALL;

    static {
        for (Lang l : LANGS) {
            INDEX.put(l.code.toLowerCase(Locale.ROOT), l);
        }
        ALL = Collections.unmodifiableList(new ArrayList<>(java.util.Arrays.asList(LANGS)));
    }

    /** 全部语种（已按使用频率排序） */
    public static List<Lang> all() {
        return ALL;
    }

    /** 按 code 查找，未收录返回 null */
    public static Lang find(String code) {
        if (code == null) return null;
        return INDEX.get(code.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 把任意写法归一到目录里的标准 code。
     * 用户可能传 en-US、zh_Hans、zh 等，这里尽量收敛，
     * 归一化失败就原样返回（交给下游 provider 自己判断）。
     */
    public static String normalize(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String s = code.trim().replace('_', '-');
        String lower = s.toLowerCase(Locale.ROOT);
        if (INDEX.containsKey(lower)) return INDEX.get(lower).code;

        // zh / zh-Hans / zh-Hant / zh-cn 等中文变体收敛
        if (lower.startsWith("zh")) {
            if (lower.contains("hant") || lower.contains("tw") || lower.contains("hk") || lower.contains("mo")) {
                return "zh-TW";
            }
            return "zh-CN";
        }
        // 取主语言子标签（en-US → en）再查一次
        String base = lower.split("-")[0];
        if (INDEX.containsKey(base)) return INDEX.get(base).code;
        return s;
    }

    /** 取展示名：优先母语名 */
    public static String displayName(String code) {
        Lang l = find(normalize(code));
        return l == null ? code : l.nativeName;
    }
}
