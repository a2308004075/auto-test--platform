/**
 * @author HXN
 * @date 2026-08-23
 * @description 自动化用例分组管理控制器
 */
package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.execution.dto.AutoCaseGroupCreateRequest;
import com.platform.execution.dto.AutoCaseGroupResponse;
import com.platform.execution.dto.AutoCaseGroupUpdateRequest;
import com.platform.execution.service.AutoCaseGroupService;
import com.platform.execution.service.AutoCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 自动化用例分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/auto-case-groups")
@RequiredArgsConstructor
public class AutoCaseGroupController {

    private final AutoCaseGroupService autoCaseGroupService;
    private final AutoCaseService autoCaseService;

    /**
     * 查询分组列表
     */
    @GetMapping
    public ApiResponse<List<AutoCaseGroupResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(autoCaseGroupService.listByProject(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<AutoCaseGroupResponse> create(@PathVariable Long projectId,
                                                     @Valid @RequestBody AutoCaseGroupCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(autoCaseGroupService.create(request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<AutoCaseGroupResponse> update(@PathVariable Long projectId,
                                                     @PathVariable Long groupId,
                                                     @Valid @RequestBody AutoCaseGroupUpdateRequest request) {
        return ApiResponse.ok(autoCaseGroupService.update(groupId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                    @PathVariable Long groupId) {
        autoCaseGroupService.delete(groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空分组及其子孙分组中的所有自动化用例
     */
    @PostMapping("/{groupId}/clear-cases")
    public ApiResponse<Void> clearCases(@PathVariable Long projectId,
                                        @PathVariable Long groupId) {
        autoCaseService.clearByGroup(projectId, groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空项目下所有自动化用例
     */
    @PostMapping("/clear-all-cases")
    public ApiResponse<Void> clearAllCases(@PathVariable Long projectId) {
        autoCaseService.clearByProject(projectId);
        return ApiResponse.ok();
    }
}
