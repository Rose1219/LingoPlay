package com.lingolearn.dto;

/** 翻译结果 */
public class TranslateResponse {

    /** 译文 */
    private String translatedText;

    /** 自动检测到的源语种（请求显式指定时为 null） */
    private String detectedLanguage;

    private String source;
    private String target;

    /** 实际生效的翻译源，便于排查质量问题 */
    private String provider;

    public TranslateResponse() {}

    public TranslateResponse(String translatedText, String detectedLanguage,
                             String source, String target, String provider) {
        this.translatedText = translatedText;
        this.detectedLanguage = detectedLanguage;
        this.source = source;
        this.target = target;
        this.provider = provider;
    }

    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }
    public String getDetectedLanguage() { return detectedLanguage; }
    public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
