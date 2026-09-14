/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件管理控制器
 */
package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.AutoSuiteCaseLifecycleDTO;
import com.platform.execution.dto.AutoSuiteCaseLifecycleSaveRequest;
import com.platform.execution.dto.AutoSuiteCreateRequest;
import com.platform.execution.dto.AutoSuitePassRateDTO;
import com.platform.execution.dto.AutoSuiteResponse;
import com.platform.execution.dto.AutoSuiteUpdateRequest;
import com.platform.execution.service.AutoSuiteCaseLifecycleService;
import com.platform.execution.service.AutoSuiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 自动化套件管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/auto-suites")
@RequiredArgsConstructor
public class AutoSuiteController {

    private final AutoSuiteService autoSuiteService;
    private final AutoSuiteCaseLifecycleService autoSuiteCaseLifecycleService;

    /**
     * 分页查询自动化套件
     *
     * @param groupId  分组 ID（null 不过滤；0/-1 表示未分组；正数=指定分组含子孙分组）
     * @param priority 优先级（P0-P3，null 不过滤）
     */
    @GetMapping
    public ApiResponse<PageResponse<AutoSuiteResponse>> list(@PathVariable Long projectId,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Long groupId,
                                                             @RequestParam(required = false) String priority,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(autoSuiteService.listAutoSuites(projectId, keyword, groupId, priority, page, pageSize));
    }

    /**
     * 创建自动化套件
     */
    @PostMapping
    public ApiResponse<AutoSuiteResponse> create(@PathVariable Long projectId,
                                                 @Valid @RequestBody AutoSuiteCreateRequest request) {
        return ApiResponse.ok(autoSuiteService.createAutoSuite(projectId, request));
    }

    /**
     * 获取自动化套件详情
     */
    @GetMapping("/{autoSuiteId}")
    public ApiResponse<AutoSuiteResponse> get(@PathVariable Long projectId,
                                              @PathVariable Long autoSuiteId) {
        return ApiResponse.ok(autoSuiteService.getAutoSuite(autoSuiteId));
    }

    /**
     * 更新自动化套件
     */
    @PostMapping("/{autoSuiteId}")
    public ApiResponse<AutoSuiteResponse> update(@PathVariable Long projectId,
                                                 @PathVariable Long autoSuiteId,
                                                 @Valid @RequestBody AutoSuiteUpdateRequest request) {
        return ApiResponse.ok(autoSuiteService.updateAutoSuite(autoSuiteId, request));
    }

    /**
     * 删除自动化套件
     */
    @PostMapping("/{autoSuiteId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                    @PathVariable Long autoSuiteId) {
        autoSuiteService.deleteAutoSuite(autoSuiteId);
        return ApiResponse.ok();
    }

    /**
     * 批量查询自动化套件通过率
     */
    @PostMapping("/pass-rates")
    public ApiResponse<List<AutoSuitePassRateDTO>> getPassRates(@PathVariable Long projectId,
                                                               @RequestBody List<Long> autoSuiteIds) {
        return ApiResponse.ok(autoSuiteService.getPassRates(projectId, autoSuiteIds));
    }

    /**
     * 批量修改自动化套件分组
     */
    @PostMapping("/batch-group")
    public ApiResponse<Void> batchUpdateGroup(@PathVariable Long projectId,
                                              @RequestBody BatchGroupRequest request) {
        autoSuiteService.batchUpdateGroup(request.getAutoSuiteIds(), request.getGroupId());
        return ApiResponse.ok();
    }

    /**
     * 查询自动化套件内自动化用例级生命周期配置
     */
    @GetMapping("/{autoSuiteId}/lifecycle")
    public ApiResponse<List<AutoSuiteCaseLifecycleDTO>> listLifecycle(@PathVariable Long projectId,
                                                                      @PathVariable Long autoSuiteId) {
        return ApiResponse.ok(autoSuiteCaseLifecycleService.listByAutoSuite(autoSuiteId));
    }

    /**
     * 保存自动化套件内自动化用例级生命周期配置
     */
    @PutMapping("/{autoSuiteId}/lifecycle")
    public ApiResponse<Void> saveLifecycle(@PathVariable Long projectId,
                                           @PathVariable Long autoSuiteId,
                                           @RequestBody AutoSuiteCaseLifecycleSaveRequest request) {
        autoSuiteCaseLifecycleService.saveLifecycle(autoSuiteId, request);
        return ApiResponse.ok();
    }

    /**
     * 批量修改分组请求体
     */
    @lombok.Data
    public static class BatchGroupRequest {
        private List<Long> autoSuiteIds;
        private Long groupId;
    }
}
