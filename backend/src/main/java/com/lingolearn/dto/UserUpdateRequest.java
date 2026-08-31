package com.lingolearn.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/** 更新个人资料请求 */
@Data
public class UserUpdateRequest {

    @Size(max = 20, message = "昵称最长 20 位")
    private String nickname;

    /** 偏好语言代码，逗号分隔，如 en,ja */
    @Size(max = 50, message = "偏好语言参数过长")
    private String preferredLanguages;
}