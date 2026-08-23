/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 关键字管理控制器
 */
package com.platform.keyword.controller;

import com.platform.apidoc.dto.ApiDebugResponse;
import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import com.platform.keyword.dto.ApiKeywordCreateRequest;
import com.platform.keyword.dto.ApiKeywordDebugRequest;
import com.platform.keyword.dto.ApiKeywordResponse;
import com.platform.keyword.dto.ApiKeywordUpdateRequest;
import com.platform.keyword.service.ApiKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 接口关键字管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/keywords")
@RequiredArgsConstructor
public class ApiKeywordController {

    private final ApiKeywordService apiKeywordService;

    /**
     * 分页查询接口关键字
     */
    @GetMapping
    public ApiResponse<PageResponse<ApiKeywordResponse>> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(apiKeywordService.list(projectId, keyword, category, moduleId, page, pageSize));
    }

    /**
     * 创建接口关键字
     */
    @PostMapping
    public ApiResponse<ApiKeywordResponse> create(@PathVariable Long projectId,
                                                    @Valid @RequestBody ApiKeywordCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(apiKeywordService.create(request));
    }

    /**
     * 获取关键字详情
     */
    @GetMapping("/{keywordId}")
    public ApiResponse<ApiKeywordResponse> get(@PathVariable Long projectId,
                                                @PathVariable Long keywordId) {
        return ApiResponse.ok(apiKeywordService.getById(keywordId));
    }

    /**
     * 更新关键字
     */
    @PostMapping("/{keywordId}")
    public ApiResponse<ApiKeywordResponse> update(@PathVariable Long projectId,
                                                    @PathVariable Long keywordId,
                                                    @Valid @RequestBody ApiKeywordUpdateRequest request) {
        return ApiResponse.ok(apiKeywordService.update(keywordId, request));
    }

    /**
     * 删除关键字
     */
    @PostMapping("/{keywordId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long keywordId) {
        apiKeywordService.delete(keywordId);
        return ApiResponse.ok();
    }

    /**
     * 从接口快速生成关键字
     */
    @PostMapping("/generate")
    public ApiResponse<ApiKeywordResponse> generate(@PathVariable Long projectId,
                                                     @RequestParam Long apiId) {
        return ApiResponse.ok(apiKeywordService.generateFromApi(projectId, apiId));
    }

    /**
     * 接口关键字在线调试
     */
    @PostMapping("/{keywordId}/debug")
    public ApiResponse<ApiDebugResponse> debug(@PathVariable Long projectId,
                                                @PathVariable Long keywordId,
                                                @Valid @RequestBody ApiKeywordDebugRequest request) {
        return ApiResponse.ok(apiKeywordService.debug(keywordId, request));
    }
}
