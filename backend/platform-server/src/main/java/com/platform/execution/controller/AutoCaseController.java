/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化用例管理控制器
 */
package com.platform.execution.controller;

import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.AutoCaseCreateRequest;
import com.platform.execution.dto.AutoCaseDebugRequest;
import com.platform.execution.dto.AutoCaseDebugResponse;
import com.platform.execution.dto.AutoCaseResponse;
import com.platform.execution.dto.AutoCaseUpdateRequest;
import com.platform.execution.service.AutoCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 自动化用例管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/auto-cases")
@RequiredArgsConstructor
public class AutoCaseController {

    private final AutoCaseService autoCaseService;

    /**
     * 分页查询自动化用例（支持按自动化套件、分组、优先级、状态筛选）
     */
    @GetMapping
    public ApiResponse<PageResponse<AutoCaseResponse>> list(@PathVariable Long projectId,
                                                            @RequestParam(required = false) Long autoSuiteId,
                                                            @RequestParam(required = false) Long groupId,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String priority,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(autoCaseService.listAutoCases(projectId, autoSuiteId, groupId, keyword, priority, status, page, pageSize));
    }

    /**
     * 创建自动化用例
     */
    @PostMapping
    public ApiResponse<AutoCaseResponse> create(@PathVariable Long projectId,
                                                @Valid @RequestBody AutoCaseCreateRequest request) {
        if (request.getAutoSuiteId() == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "autoSuiteId 不能为空");
        }
        return ApiResponse.ok(autoCaseService.createAutoCase(request.getAutoSuiteId(), request));
    }

    /**
     * 获取自动化用例详情
     */
    @GetMapping("/{autoCaseId}")
    public ApiResponse<AutoCaseResponse> get(@PathVariable Long projectId,
                                             @PathVariable Long autoCaseId) {
        return ApiResponse.ok(autoCaseService.getAutoCase(autoCaseId));
    }

    /**
     * 更新自动化用例
     */
    @PostMapping("/{autoCaseId}")
    public ApiResponse<AutoCaseResponse> update(@PathVariable Long projectId,
                                                @PathVariable Long autoCaseId,
                                                @Valid @RequestBody AutoCaseUpdateRequest request) {
        return ApiResponse.ok(autoCaseService.updateAutoCase(autoCaseId, request));
    }

    /**
     * 删除自动化用例
     */
    @PostMapping("/{autoCaseId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                    @PathVariable Long autoCaseId) {
        autoCaseService.deleteAutoCase(autoCaseId);
        return ApiResponse.ok();
    }

    /**
     * 启用/禁用自动化用例
     */
    @PostMapping("/{autoCaseId}/status")
    public ApiResponse<AutoCaseResponse> toggleStatus(@PathVariable Long projectId,
                                                      @PathVariable Long autoCaseId) {
        return ApiResponse.ok(autoCaseService.toggleStatus(autoCaseId));
    }

    /**
     * 自动化用例调试：同步执行单条自动化用例并返回详细结果
     */
    @PostMapping("/{autoCaseId}/debug")
    public ApiResponse<AutoCaseDebugResponse> debug(@PathVariable Long projectId,
                                                    @PathVariable Long autoCaseId,
                                                    @RequestBody(required = false) AutoCaseDebugRequest request) {
        if (request == null) {
            request = new AutoCaseDebugRequest();
        }
        return ApiResponse.ok(autoCaseService.debugAutoCase(autoCaseId, request));
    }
}
