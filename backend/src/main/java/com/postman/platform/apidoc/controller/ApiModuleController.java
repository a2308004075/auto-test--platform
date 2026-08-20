package com.postman.platform.apidoc.controller;

import com.postman.platform.apidoc.dto.ApiModuleCreateRequest;
import com.postman.platform.apidoc.dto.ApiModuleResponse;
import com.postman.platform.apidoc.dto.ApiModuleUpdateRequest;
import com.postman.platform.apidoc.service.ApiModuleService;
import com.postman.platform.common.response.ApiResponse;
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
    @PutMapping("/{moduleId}")
    public ApiResponse<ApiModuleResponse> update(@PathVariable Long projectId,
                                                  @PathVariable Long moduleId,
                                                  @Valid @RequestBody ApiModuleUpdateRequest request) {
        return ApiResponse.ok(apiModuleService.update(moduleId, request));
    }

    /**
     * 删除分组
     */
    @DeleteMapping("/{moduleId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long moduleId) {
        apiModuleService.delete(moduleId);
        return ApiResponse.ok();
    }
}
