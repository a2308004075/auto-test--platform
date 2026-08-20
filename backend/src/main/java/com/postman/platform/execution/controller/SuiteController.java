package com.postman.platform.execution.controller;

import com.postman.platform.common.response.ApiResponse;
import com.postman.platform.common.response.PageResponse;
import com.postman.platform.execution.dto.SuiteCreateRequest;
import com.postman.platform.execution.dto.SuiteResponse;
import com.postman.platform.execution.dto.SuiteUpdateRequest;
import com.postman.platform.execution.service.SuiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 测试套件管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/suites")
@RequiredArgsConstructor
public class SuiteController {

    private final SuiteService suiteService;

    /**
     * 分页查询测试套件
     */
    @GetMapping
    public ApiResponse<PageResponse<SuiteResponse>> list(@PathVariable String projectId,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(suiteService.listSuites(projectId, keyword, page, pageSize));
    }

    /**
     * 创建测试套件
     */
    @PostMapping
    public ApiResponse<SuiteResponse> create(@PathVariable String projectId,
                                             @Valid @RequestBody SuiteCreateRequest request) {
        return ApiResponse.ok(suiteService.createSuite(projectId, request));
    }

    /**
     * 获取套件详情
     */
    @GetMapping("/{suiteId}")
    public ApiResponse<SuiteResponse> get(@PathVariable String projectId,
                                          @PathVariable String suiteId) {
        return ApiResponse.ok(suiteService.getSuite(suiteId));
    }

    /**
     * 更新测试套件
     */
    @PutMapping("/{suiteId}")
    public ApiResponse<SuiteResponse> update(@PathVariable String projectId,
                                             @PathVariable String suiteId,
                                             @Valid @RequestBody SuiteUpdateRequest request) {
        return ApiResponse.ok(suiteService.updateSuite(suiteId, request));
    }

    /**
     * 删除测试套件
     */
    @DeleteMapping("/{suiteId}")
    public ApiResponse<Void> delete(@PathVariable String projectId,
                                     @PathVariable String suiteId) {
        suiteService.deleteSuite(suiteId);
        return ApiResponse.ok();
    }
}
