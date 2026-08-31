package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.dto.AuthResponse;
import com.lingolearn.dto.LoginRequest;
import com.lingolearn.dto.RegisterRequest;
import com.lingolearn.dto.WxLoginRequest;
import com.lingolearn.service.AuthService;
import com.lingolearn.service.WxAuthService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/** 认证接口 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final WxAuthService wxAuthService;

    public AuthController(AuthService authService, WxAuthService wxAuthService) {
        this.authService = authService;
        this.wxAuthService = wxAuthService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    /** 小程序微信授权登录 */
    @PostMapping("/wx-login")
    public ApiResponse<AuthResponse> wxLogin(@Valid @RequestBody WxLoginRequest req) {
        return ApiResponse.ok(wxAuthService.loginByCode(req.getCode()));
    }
}