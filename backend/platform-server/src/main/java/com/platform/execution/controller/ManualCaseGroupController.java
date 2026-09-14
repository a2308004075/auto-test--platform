/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动化用例分组管理控制器
 */
package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.execution.dto.ManualCaseGroupCreateRequest;
import com.platform.execution.dto.ManualCaseGroupResponse;
import com.platform.execution.dto.ManualCaseGroupUpdateRequest;
import com.platform.execution.service.ManualCaseGroupService;
import com.platform.execution.service.ManualCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 手动化用例分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/manual-case-groups")
@RequiredArgsConstructor
public class ManualCaseGroupController {

    private final ManualCaseGroupService manualCaseGroupService;
    private final ManualCaseService manualCaseService;

    /**
     * 查询分组列表
     */
    @GetMapping
    public ApiResponse<List<ManualCaseGroupResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(manualCaseGroupService.listByProject(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<ManualCaseGroupResponse> create(@PathVariable Long projectId,
                                                        @Valid @RequestBody ManualCaseGroupCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(manualCaseGroupService.create(request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<ManualCaseGroupResponse> update(@PathVariable Long projectId,
                                                        @PathVariable Long groupId,
                                                        @Valid @RequestBody ManualCaseGroupUpdateRequest request) {
        return ApiResponse.ok(manualCaseGroupService.update(groupId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long groupId) {
        manualCaseGroupService.delete(groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空分组及其子孙分组中的所有手动化用例
     */
    @PostMapping("/{groupId}/clear-cases")
    public ApiResponse<Void> clearCases(@PathVariable Long projectId,
                                         @PathVariable Long groupId) {
        manualCaseService.clearByGroup(projectId, groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空项目下所有手动化用例
     */
    @PostMapping("/clear-all-cases")
    public ApiResponse<Void> clearAllCases(@PathVariable Long projectId) {
        manualCaseService.clearByProject(projectId);
        return ApiResponse.ok();
    }
}
