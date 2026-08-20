package com.platform.auth.controller;

import com.platform.auth.dto.GlobalConfigResponse;
import com.platform.auth.dto.GlobalConfigUpdateRequest;
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
@PreAuthorize("hasRole('ADMIN')")
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
    @PutMapping("/{configKey}")
    public ApiResponse<GlobalConfigResponse> update(@PathVariable String configKey,
                                                    @Valid @RequestBody GlobalConfigUpdateRequest request) {
        return ApiResponse.ok(settingsService.update(configKey, request));
    }
}
