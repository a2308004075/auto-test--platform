package com.platform.auth.controller;

import com.platform.auth.dto.*;
import com.platform.auth.entity.User;
import com.platform.auth.service.AuthService;
import com.platform.auth.service.CaptchaService;
import com.platform.common.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    /**
     * 获取验证码图片
     */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.success(captchaService.generateCaptcha());
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * 刷新 Access Token
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ApiResponse.success(response);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        String token = null;
        if (bearer != null && bearer.startsWith(BEARER_PREFIX)) {
            token = bearer.substring(BEARER_PREFIX.length());
        }
        authService.logout(token);
        return ApiResponse.success(null, "登出成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal User user) {
        UserResponse response = authService.getCurrentUser(user.getId());
        return ApiResponse.success(response);
    }
}
