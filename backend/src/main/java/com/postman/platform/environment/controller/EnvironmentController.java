package com.postman.platform.environment.controller;

import com.postman.platform.common.response.ApiResponse;
import com.postman.platform.environment.dto.*;
import com.postman.platform.environment.service.EnvironmentService;
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
    public ApiResponse<List<EnvironmentResponse>> list(@PathVariable String projectId) {
        return ApiResponse.ok(environmentService.listByProject(projectId));
    }

    /**
     * 创建环境
     */
    @PostMapping
    public ApiResponse<EnvironmentResponse> create(@PathVariable String projectId,
                                                    @Valid @RequestBody EnvironmentCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(environmentService.create(request));
    }

    /**
     * 更新环境
     */
    @PutMapping("/{envId}")
    public ApiResponse<EnvironmentResponse> update(@PathVariable String projectId,
                                                    @PathVariable String envId,
                                                    @Valid @RequestBody EnvironmentUpdateRequest request) {
        return ApiResponse.ok(environmentService.update(envId, request));
    }

    /**
     * 删除环境
     */
    @DeleteMapping("/{envId}")
    public ApiResponse<Void> delete(@PathVariable String projectId,
                                     @PathVariable String envId) {
        environmentService.delete(envId);
        return ApiResponse.ok();
    }

    /**
     * 激活/取消激活环境
     */
    @PatchMapping("/{envId}/activate")
    public ApiResponse<EnvironmentResponse> activate(@PathVariable String projectId,
                                                      @PathVariable String envId) {
        return ApiResponse.ok(environmentService.activate(envId));
    }

    /**
     * 测试环境连接
     */
    @PostMapping("/{envId}/test")
    public ApiResponse<TestResult> testConnection(@PathVariable String projectId,
                                                   @PathVariable String envId) {
        return ApiResponse.ok(environmentService.testConnection(envId));
    }

    /**
     * 获取环境详情
     */
    @GetMapping("/{envId}")
    public ApiResponse<EnvironmentResponse> getDetail(@PathVariable String projectId,
                                                       @PathVariable String envId) {
        return ApiResponse.ok(environmentService.getDetail(envId));
    }
}
