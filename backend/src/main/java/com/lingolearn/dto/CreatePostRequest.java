package com.lingolearn.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 发帖请求 */
@Data
public class CreatePostRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 5000, message = "内容最长 5000 字")
    private String content;

    /** 关联语种代码，可为空 */
    private String languageCode;
}