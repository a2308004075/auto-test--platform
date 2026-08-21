package com.platform.auth.controller;

import com.platform.auth.dto.*;
import com.platform.auth.entity.User;
import com.platform.auth.service.AuthService;
import com.platform.auth.service.CaptchaService;
import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
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
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, ip, userAgent);
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

    /**
     * 更新个人资料（当前用户修改自己的资料）
     */
    @PostMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(@AuthenticationPrincipal User user,
                                                    @Valid @RequestBody ProfileUpdateRequest request) {
        UserResponse response = authService.updateProfile(user.getId(), request);
        return ApiResponse.success(response, "资料更新成功");
    }

    /**
     * 修改密码（当前用户修改自己的密码）
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal User user,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
        return ApiResponse.success(null, "密码修改成功");
    }

    /**
     * 查询当前用户的登录日志（最近 30 天）
     */
    @GetMapping("/login-logs")
    public ApiResponse<PageResponse<LoginLogResponse>> loginLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<LoginLogResponse> response = authService.getLoginLogs(user.getId(), page, pageSize);
        return ApiResponse.success(response);
    }

    /**
     * 从请求中提取客户端 IP（处理反向代理头）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能有多个值，取第一个
            int commaIndex = ip.indexOf(',');
            return commaIndex > 0 ? ip.substring(0, commaIndex).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
