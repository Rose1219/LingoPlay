package com.lingolearn.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 小程序微信登录请求 */
@Data
public class WxLoginRequest {

    /** wx.login() 获得的临时登录凭证 */
    @NotBlank(message = "code 不能为空")
    private String code;
}