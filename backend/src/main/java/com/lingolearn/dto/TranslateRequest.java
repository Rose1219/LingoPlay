package com.lingolearn.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 翻译请求 */
public class TranslateRequest {

    @NotBlank(message = "待翻译文本不能为空")
    @Size(max = 2000, message = "单次翻译最多 2000 字")
    private String text;

    /** 源语种，auto / 留空 表示自动检测 */
    private String source;

    /** 目标语种，必填 */
    private String target;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
}
