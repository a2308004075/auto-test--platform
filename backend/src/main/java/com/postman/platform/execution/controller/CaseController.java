package com.postman.platform.execution.controller;

import com.postman.platform.common.exception.BusinessException;
import com.postman.platform.common.exception.ErrorCode;
import com.postman.platform.common.response.ApiResponse;
import com.postman.platform.common.response.PageResponse;
import com.postman.platform.execution.dto.CaseCreateRequest;
import com.postman.platform.execution.dto.CaseResponse;
import com.postman.platform.execution.dto.CaseUpdateRequest;
import com.postman.platform.execution.service.CaseService;
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
     * 分页查询测试用例（支持按套件筛选）
     */
    @GetMapping
    public ApiResponse<PageResponse<CaseResponse>> list(@PathVariable String projectId,
                                                         @RequestParam(required = false) String suiteId,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(caseService.listCases(suiteId, keyword, page, pageSize));
    }

    /**
     * 创建测试用例
     */
    @PostMapping
    public ApiResponse<CaseResponse> create(@PathVariable String projectId,
                                             @Valid @RequestBody CaseCreateRequest request) {
        if (!StringUtils.hasText(request.getSuiteId())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "suiteId 不能为空");
        }
        return ApiResponse.ok(caseService.createCase(request.getSuiteId(), request));
    }

    /**
     * 获取用例详情
     */
    @GetMapping("/{caseId}")
    public ApiResponse<CaseResponse> get(@PathVariable String projectId,
                                          @PathVariable String caseId) {
        return ApiResponse.ok(caseService.getCase(caseId));
    }

    /**
     * 更新测试用例
     */
    @PutMapping("/{caseId}")
    public ApiResponse<CaseResponse> update(@PathVariable String projectId,
                                             @PathVariable String caseId,
                                             @Valid @RequestBody CaseUpdateRequest request) {
        return ApiResponse.ok(caseService.updateCase(caseId, request));
    }

    /**
     * 删除测试用例
     */
    @DeleteMapping("/{caseId}")
    public ApiResponse<Void> delete(@PathVariable String projectId,
                                     @PathVariable String caseId) {
        caseService.deleteCase(caseId);
        return ApiResponse.ok();
    }

    /**
     * 启用/禁用测试用例
     */
    @PatchMapping("/{caseId}/status")
    public ApiResponse<CaseResponse> toggleStatus(@PathVariable String projectId,
                                                   @PathVariable String caseId) {
        return ApiResponse.ok(caseService.toggleStatus(caseId));
    }
}
