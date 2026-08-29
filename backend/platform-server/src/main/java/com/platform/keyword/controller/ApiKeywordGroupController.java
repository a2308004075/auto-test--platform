/**
 * @author HXN
 * @date 2026-08-26
 * @description 接口关键字分组管理控制器
 */
package com.platform.keyword.controller;

import com.platform.common.response.ApiResponse;
import com.platform.keyword.dto.ApiKeywordGroupCreateRequest;
import com.platform.keyword.dto.ApiKeywordGroupResponse;
import com.platform.keyword.dto.ApiKeywordGroupUpdateRequest;
import com.platform.keyword.service.ApiKeywordGroupService;
import com.platform.keyword.service.ApiKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 接口关键字分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/keyword-groups")
@RequiredArgsConstructor
public class ApiKeywordGroupController {

    private final ApiKeywordGroupService apiKeywordGroupService;
    private final ApiKeywordService apiKeywordService;

    /**
     * 查询分组列表
     */
    @GetMapping
    public ApiResponse<List<ApiKeywordGroupResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(apiKeywordGroupService.listByProject(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<ApiKeywordGroupResponse> create(@PathVariable Long projectId,
                                                       @Valid @RequestBody ApiKeywordGroupCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(apiKeywordGroupService.create(request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<ApiKeywordGroupResponse> update(@PathVariable Long projectId,
                                                       @PathVariable Long groupId,
                                                       @Valid @RequestBody ApiKeywordGroupUpdateRequest request) {
        return ApiResponse.ok(apiKeywordGroupService.update(groupId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                    @PathVariable Long groupId) {
        apiKeywordGroupService.delete(groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空分组内所有关键字（含子孙分组）
     */
    @PostMapping("/{groupId}/clear-keywords")
    public ApiResponse<Void> clearKeywords(@PathVariable Long projectId,
                                           @PathVariable Long groupId) {
        apiKeywordService.clearByGroup(groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空项目下所有接口关键字
     */
    @PostMapping("/clear-all-keywords")
    public ApiResponse<Void> clearAllKeywords(@PathVariable Long projectId) {
        apiKeywordService.clearByProject(projectId);
        return ApiResponse.ok();
    }
}
