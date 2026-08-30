/**
 * @author HXN
 * @date 2026-08-23
 * @description 测试计划分组控制器
 */
package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.execution.dto.PlanGroupRequest;
import com.platform.execution.dto.PlanGroupResponse;
import com.platform.execution.service.PlanGroupService;
import com.platform.execution.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 测试计划分组管理接口
 */
@RestController
@RequiredArgsConstructor
public class PlanGroupController {

    private final PlanGroupService planGroupService;
    private final PlanService planService;

    /**
     * 获取项目下的分组列表
     */
    @GetMapping("/api/v1/projects/{projectId}/plan-groups")
    public ApiResponse<List<PlanGroupResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(planGroupService.listByProject(projectId));
    }

    /**
     * 新建分组
     */
    @PostMapping("/api/v1/projects/{projectId}/plan-groups")
    public ApiResponse<PlanGroupResponse> create(@PathVariable Long projectId,
                                                  @Valid @RequestBody PlanGroupRequest request) {
        return ApiResponse.ok(planGroupService.create(projectId, request));
    }

    /**
     * 编辑分组
     */
    @PostMapping("/api/v1/plan-groups/{groupId}")
    public ApiResponse<PlanGroupResponse> update(@PathVariable Long groupId,
                                                  @Valid @RequestBody PlanGroupRequest request) {
        return ApiResponse.ok(planGroupService.update(groupId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/api/v1/plan-groups/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long groupId) {
        planGroupService.delete(groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空分组及其子孙分组中的所有计划
     */
    @PostMapping("/api/v1/projects/{projectId}/plan-groups/{groupId}/clear-plans")
    public ApiResponse<Void> clearPlans(@PathVariable Long projectId,
                                        @PathVariable Long groupId) {
        planService.clearByGroup(projectId, groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空项目下所有计划
     */
    @PostMapping("/api/v1/projects/{projectId}/plan-groups/clear-all-plans")
    public ApiResponse<Void> clearAllPlans(@PathVariable Long projectId) {
        planService.clearByProject(projectId);
        return ApiResponse.ok();
    }
}
