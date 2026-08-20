package com.postman.platform.apidoc.controller;

import com.postman.platform.apidoc.dto.*;
import com.postman.platform.apidoc.service.ApiService;
import com.postman.platform.common.response.ApiResponse;
import com.postman.platform.common.response.PageResponse;
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
            @PathVariable String projectId,
            @RequestParam(required = false) String moduleId,
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
    public ApiResponse<ApiInfoResponse> create(@PathVariable String projectId,
                                                @Valid @RequestBody ApiCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(apiService.create(request));
    }

    /**
     * 更新接口
     */
    @PutMapping("/{apiId}")
    public ApiResponse<ApiInfoResponse> update(@PathVariable String projectId,
                                                @PathVariable String apiId,
                                                @Valid @RequestBody ApiUpdateRequest request) {
        return ApiResponse.ok(apiService.update(apiId, request));
    }

    /**
     * 获取接口详情
     */
    @GetMapping("/{apiId}")
    public ApiResponse<ApiInfoResponse> get(@PathVariable String projectId,
                                             @PathVariable String apiId) {
        return ApiResponse.ok(apiService.getById(apiId));
    }

    /**
     * 删除接口
     */
    @DeleteMapping("/{apiId}")
    public ApiResponse<Void> delete(@PathVariable String projectId,
                                     @PathVariable String apiId) {
        apiService.delete(apiId);
        return ApiResponse.ok();
    }

    /**
     * 批量删除接口
     */
    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@PathVariable String projectId,
                                          @RequestBody List<String> apiIds) {
        apiService.batchDelete(apiIds);
        return ApiResponse.ok();
    }

    /**
     * 批量移动接口
     */
    @PostMapping("/batch-move")
    public ApiResponse<Void> batchMove(@PathVariable String projectId,
                                        @RequestParam String targetModuleId,
                                        @RequestBody List<String> apiIds) {
        apiService.batchMove(apiIds, targetModuleId);
        return ApiResponse.ok();
    }

    /**
     * Swagger 导入
     */
    @PostMapping("/swagger-import")
    public ApiResponse<SwaggerImportResult> swaggerImport(@PathVariable String projectId,
                                                           @Valid @RequestBody SwaggerImportRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(apiService.importSwagger(request));
    }

    /**
     * 接口调试
     */
    @PostMapping("/{apiId}/debug")
    public ApiResponse<ApiDebugResponse> debug(@PathVariable String projectId,
                                                @PathVariable String apiId,
                                                @Valid @RequestBody ApiDebugRequest request) {
        return ApiResponse.ok(apiService.debug(apiId, request));
    }
}
