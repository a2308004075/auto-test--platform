/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 系统设置控制器
 */
package com.platform.auth.controller;

import com.platform.auth.dto.GlobalConfigResponse;
import com.platform.auth.dto.GlobalConfigUpdateRequest;
import com.platform.auth.dto.TestSendRequest;
import com.platform.auth.service.SettingsService;
import com.platform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 全局配置管理接口（仅 ADMIN）
 */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * 查询全部全局配置
     */
    @GetMapping
    public ApiResponse<List<GlobalConfigResponse>> list() {
        return ApiResponse.ok(settingsService.listAll());
    }

    /**
     * 查询单个配置
     */
    @GetMapping("/{configKey}")
    public ApiResponse<GlobalConfigResponse> get(@PathVariable String configKey) {
        return ApiResponse.ok(settingsService.getByKey(configKey));
    }

    /**
     * 更新配置项
     */
    @PostMapping("/{configKey}")
    public ApiResponse<GlobalConfigResponse> update(@PathVariable String configKey,
                                                    @Valid @RequestBody GlobalConfigUpdateRequest request) {
        return ApiResponse.ok(settingsService.update(configKey, request));
    }

    /**
     * 测试 SMTP 邮件发送
     */
    @PostMapping("/test-smtp")
    public ApiResponse<String> testSmtpSend(@Valid @RequestBody TestSendRequest request) {
        String result = settingsService.testSmtpSend(request);
        return ApiResponse.success(result, "测试邮件发送成功");
    }

    /**
     * 测试 Webhook 通知发送
     */
    @PostMapping("/test-webhook")
    public ApiResponse<String> testWebhookSend(@Valid @RequestBody TestSendRequest request) {
        String result = settingsService.testWebhookSend(request);
        return ApiResponse.success(result, "Webhook 通知发送成功");
    }
}
