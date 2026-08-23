/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 接口管理控制器
 */
package com.platform.apidoc.controller;

import com.platform.apidoc.dto.*;
import com.platform.apidoc.service.ApiService;
import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 接口文档管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/apis")
@RequiredArgsConstructor
public class ApiController {

    private final ApiService apiService;

    /**
     * 分页查询接口列表
     */
    @GetMapping
    public ApiResponse<PageResponse<ApiInfoResponse>> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(apiService.list(projectId, moduleId, keyword, httpMethod, page, pageSize));
    }

    /**
     * 创建接口
     */
    @PostMapping
    public ApiResponse<ApiInfoResponse> create(@PathVariable Long projectId,
                                                @Valid @RequestBody ApiCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(apiService.create(request));
    }

    /**
     * 更新接口
     */
    @PostMapping("/{apiId}")
    public ApiResponse<ApiInfoResponse> update(@PathVariable Long projectId,
                                                @PathVariable Long apiId,
                                                @Valid @RequestBody ApiUpdateRequest request) {
        return ApiResponse.ok(apiService.update(apiId, request));
    }

    /**
     * 获取接口详情
     */
    @GetMapping("/{apiId}")
    public ApiResponse<ApiInfoResponse> get(@PathVariable Long projectId,
                                             @PathVariable Long apiId) {
        return ApiResponse.ok(apiService.getById(apiId));
    }

    /**
     * 删除接口
     */
    @PostMapping("/{apiId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long apiId) {
        apiService.delete(apiId);
        return ApiResponse.ok();
    }

    /**
     * 批量删除接口
     */
    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@PathVariable Long projectId,
                                          @RequestBody List<Long> apiIds) {
        apiService.batchDelete(apiIds);
        return ApiResponse.ok();
    }

    /**
     * 批量移动接口
     */
    @PostMapping("/batch-move")
    public ApiResponse<Void> batchMove(@PathVariable Long projectId,
                                        @RequestParam Long targetModuleId,
                                        @RequestBody List<Long> apiIds) {
        apiService.batchMove(apiIds, targetModuleId);
        return ApiResponse.ok();
    }

    /**
     * Swagger 导入
     */
    @PostMapping("/swagger-import")
    public ApiResponse<SwaggerImportResult> swaggerImport(@PathVariable Long projectId,
                                                           @Valid @RequestBody SwaggerImportRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(apiService.importSwagger(request));
    }

    /**
     * 从 URL 同步 Swagger 文档（增量导入）
     */
    @PostMapping("/swagger-sync-url")
    public ApiResponse<SwaggerImportResult> swaggerSyncUrl(@PathVariable Long projectId,
                                                            @Valid @RequestBody SwaggerSyncRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(apiService.syncFromUrl(request));
    }

    /**
     * 接口调试
     */
    @PostMapping("/{apiId}/debug")
    public ApiResponse<ApiDebugResponse> debug(@PathVariable Long projectId,
                                                @PathVariable Long apiId,
                                                @Valid @RequestBody ApiDebugRequest request) {
        return ApiResponse.ok(apiService.debug(apiId, request));
    }

    /**
     * 查询接口被关键字引用的关系
     */
    @GetMapping("/{apiId}/references")
    public ApiResponse<List<ApiReferenceResponse>> getReferences(@PathVariable Long projectId,
                                                                  @PathVariable Long apiId) {
        return ApiResponse.ok(apiService.getReferences(apiId));
    }
}
