package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.PlanCreateRequest;
import com.platform.execution.dto.PlanResponse;
import com.platform.execution.dto.PlanUpdateRequest;
import com.platform.execution.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 测试计划管理接口
 */
@RestController
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /**
     * 分页查询测试计划
     */
    @GetMapping("/api/v1/projects/{projectId}/plans")
    public ApiResponse<PageResponse<PlanResponse>> list(@PathVariable Long projectId,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(planService.listPlans(projectId, keyword, page, pageSize));
    }

    /**
     * 创建测试计划
     */
    @PostMapping("/api/v1/projects/{projectId}/plans")
    public ApiResponse<PlanResponse> create(@PathVariable Long projectId,
                                            @Valid @RequestBody PlanCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(planService.createPlan(request));
    }

    /**
     * 获取计划详情
     */
    @GetMapping("/api/v1/plans/{planId}")
    public ApiResponse<PlanResponse> get(@PathVariable Long planId) {
        return ApiResponse.ok(planService.getPlan(planId));
    }

    /**
     * 更新计划
     */
    @PostMapping("/api/v1/plans/{planId}")
    public ApiResponse<PlanResponse> update(@PathVariable Long planId,
                                            @Valid @RequestBody PlanUpdateRequest request) {
        return ApiResponse.ok(planService.updatePlan(planId, request));
    }

    /**
     * 删除计划
     */
    @PostMapping("/api/v1/plans/{planId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long planId) {
        planService.deletePlan(planId);
        return ApiResponse.ok();
    }
}
