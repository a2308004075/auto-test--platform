/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 模块管理控制器
 */
package com.platform.apidoc.controller;

import com.platform.apidoc.dto.ApiModuleCreateRequest;
import com.platform.apidoc.dto.ApiModuleResponse;
import com.platform.apidoc.dto.ApiModuleUpdateRequest;
import com.platform.apidoc.service.ApiModuleService;
import com.platform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 接口分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/modules")
@RequiredArgsConstructor
public class ApiModuleController {

    private final ApiModuleService apiModuleService;

    /**
     * 查询分组列表
     */
    @GetMapping
    public ApiResponse<List<ApiModuleResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(apiModuleService.listByProject(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<ApiModuleResponse> create(@PathVariable Long projectId,
                                                  @Valid @RequestBody ApiModuleCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(apiModuleService.create(request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{moduleId}")
    public ApiResponse<ApiModuleResponse> update(@PathVariable Long projectId,
                                                  @PathVariable Long moduleId,
                                                  @Valid @RequestBody ApiModuleUpdateRequest request) {
        return ApiResponse.ok(apiModuleService.update(moduleId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/{moduleId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long moduleId) {
        apiModuleService.delete(moduleId);
        return ApiResponse.ok();
    }
}
