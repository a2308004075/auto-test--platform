package com.postman.platform.execution.controller;

import com.postman.platform.common.response.ApiResponse;
import com.postman.platform.common.response.PageResponse;
import com.postman.platform.execution.dto.PlanCreateRequest;
import com.postman.platform.execution.dto.PlanResponse;
import com.postman.platform.execution.dto.PlanUpdateRequest;
import com.postman.platform.execution.service.PlanService;
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
    public ApiResponse<PageResponse<PlanResponse>> list(@PathVariable String projectId,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(planService.listPlans(projectId, keyword, page, pageSize));
    }

    /**
     * 创建测试计划
     */
    @PostMapping("/api/v1/projects/{projectId}/plans")
    public ApiResponse<PlanResponse> create(@PathVariable String projectId,
                                            @Valid @RequestBody PlanCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(planService.createPlan(request));
    }

    /**
     * 获取计划详情
     */
    @GetMapping("/api/v1/plans/{planId}")
    public ApiResponse<PlanResponse> get(@PathVariable String planId) {
        return ApiResponse.ok(planService.getPlan(planId));
    }

    /**
     * 更新计划
     */
    @PutMapping("/api/v1/plans/{planId}")
    public ApiResponse<PlanResponse> update(@PathVariable String planId,
                                            @Valid @RequestBody PlanUpdateRequest request) {
        return ApiResponse.ok(planService.updatePlan(planId, request));
    }

    /**
     * 删除计划
     */
    @DeleteMapping("/api/v1/plans/{planId}")
    public ApiResponse<Void> delete(@PathVariable String planId) {
        planService.deletePlan(planId);
        return ApiResponse.ok();
    }
}
