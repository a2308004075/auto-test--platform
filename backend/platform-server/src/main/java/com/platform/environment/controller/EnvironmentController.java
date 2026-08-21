package com.platform.environment.controller;

import com.platform.common.response.ApiResponse;
import com.platform.environment.dto.*;
import com.platform.environment.service.EnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 环境配置管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/environments")
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentService environmentService;

    /**
     * 查询项目下的环境列表
     */
    @GetMapping
    public ApiResponse<List<EnvironmentResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(environmentService.listByProject(projectId));
    }

    /**
     * 创建环境
     */
    @PostMapping
    public ApiResponse<EnvironmentResponse> create(@PathVariable Long projectId,
                                                    @Valid @RequestBody EnvironmentCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(environmentService.create(request));
    }

    /**
     * 更新环境
     */
    @PostMapping("/{envId}")
    public ApiResponse<EnvironmentResponse> update(@PathVariable Long projectId,
                                                    @PathVariable Long envId,
                                                    @Valid @RequestBody EnvironmentUpdateRequest request) {
        return ApiResponse.ok(environmentService.update(envId, request));
    }

    /**
     * 删除环境
     */
    @PostMapping("/{envId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long envId) {
        environmentService.delete(envId);
        return ApiResponse.ok();
    }

    /**
     * 激活/取消激活环境
     */
    @PostMapping("/{envId}/activate")
    public ApiResponse<EnvironmentResponse> activate(@PathVariable Long projectId,
                                                      @PathVariable Long envId) {
        return ApiResponse.ok(environmentService.activate(envId));
    }

    /**
     * 测试环境连接
     */
    @PostMapping("/{envId}/test")
    public ApiResponse<TestResult> testConnection(@PathVariable Long projectId,
                                                   @PathVariable Long envId) {
        return ApiResponse.ok(environmentService.testConnection(envId));
    }

    /**
     * 获取环境详情
     */
    @GetMapping("/{envId}")
    public ApiResponse<EnvironmentResponse> getDetail(@PathVariable Long projectId,
                                                       @PathVariable Long envId) {
        return ApiResponse.ok(environmentService.getDetail(envId));
    }
}
