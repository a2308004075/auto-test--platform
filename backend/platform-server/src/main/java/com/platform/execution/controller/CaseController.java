/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试用例管理控制器
 */
package com.platform.execution.controller;

import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.CaseCreateRequest;
import com.platform.execution.dto.CaseResponse;
import com.platform.execution.dto.CaseUpdateRequest;
import com.platform.execution.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 测试用例管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    /**
     * 分页查询测试用例（支持按套件、分组、优先级、状态筛选）
     */
    @GetMapping
    public ApiResponse<PageResponse<CaseResponse>> list(@PathVariable Long projectId,
                                                         @RequestParam(required = false) Long suiteId,
                                                         @RequestParam(required = false) Long groupId,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String priority,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(caseService.listCases(suiteId, groupId, keyword, priority, status, page, pageSize));
    }

    /**
     * 创建测试用例
     */
    @PostMapping
    public ApiResponse<CaseResponse> create(@PathVariable Long projectId,
                                             @Valid @RequestBody CaseCreateRequest request) {
        if (request.getSuiteId() == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "suiteId 不能为空");
        }
        return ApiResponse.ok(caseService.createCase(request.getSuiteId(), request));
    }

    /**
     * 获取用例详情
     */
    @GetMapping("/{caseId}")
    public ApiResponse<CaseResponse> get(@PathVariable Long projectId,
                                          @PathVariable Long caseId) {
        return ApiResponse.ok(caseService.getCase(caseId));
    }

    /**
     * 更新测试用例
     */
    @PostMapping("/{caseId}")
    public ApiResponse<CaseResponse> update(@PathVariable Long projectId,
                                             @PathVariable Long caseId,
                                             @Valid @RequestBody CaseUpdateRequest request) {
        return ApiResponse.ok(caseService.updateCase(caseId, request));
    }

    /**
     * 删除测试用例
     */
    @PostMapping("/{caseId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long caseId) {
        caseService.deleteCase(caseId);
        return ApiResponse.ok();
    }

    /**
     * 启用/禁用测试用例
     */
    @PostMapping("/{caseId}/status")
    public ApiResponse<CaseResponse> toggleStatus(@PathVariable Long projectId,
                                                   @PathVariable Long caseId) {
        return ApiResponse.ok(caseService.toggleStatus(caseId));
    }
}
