package com.lingolearn.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 登录请求（账号可为用户名或邮箱） */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}