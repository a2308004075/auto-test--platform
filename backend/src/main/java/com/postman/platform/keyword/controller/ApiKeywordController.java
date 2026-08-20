package com.postman.platform.keyword.controller;

import com.postman.platform.common.response.ApiResponse;
import com.postman.platform.common.response.PageResponse;
import com.postman.platform.keyword.dto.ApiKeywordCreateRequest;
import com.postman.platform.keyword.dto.ApiKeywordResponse;
import com.postman.platform.keyword.dto.ApiKeywordUpdateRequest;
import com.postman.platform.keyword.service.ApiKeywordService;
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
            @PathVariable String projectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(apiKeywordService.list(projectId, keyword, page, pageSize));
    }

    /**
     * 创建接口关键字
     */
    @PostMapping
    public ApiResponse<ApiKeywordResponse> create(@PathVariable String projectId,
                                                    @Valid @RequestBody ApiKeywordCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(apiKeywordService.create(request));
    }

    /**
     * 获取关键字详情
     */
    @GetMapping("/{keywordId}")
    public ApiResponse<ApiKeywordResponse> get(@PathVariable String projectId,
                                                @PathVariable String keywordId) {
        return ApiResponse.ok(apiKeywordService.getById(keywordId));
    }

    /**
     * 更新关键字
     */
    @PutMapping("/{keywordId}")
    public ApiResponse<ApiKeywordResponse> update(@PathVariable String projectId,
                                                    @PathVariable String keywordId,
                                                    @Valid @RequestBody ApiKeywordUpdateRequest request) {
        return ApiResponse.ok(apiKeywordService.update(keywordId, request));
    }

    /**
     * 删除关键字
     */
    @DeleteMapping("/{keywordId}")
    public ApiResponse<Void> delete(@PathVariable String projectId,
                                     @PathVariable String keywordId) {
        apiKeywordService.delete(keywordId);
        return ApiResponse.ok();
    }

    /**
     * 从接口快速生成关键字
     */
    @PostMapping("/generate")
    public ApiResponse<ApiKeywordResponse> generate(@PathVariable String projectId,
                                                     @RequestParam String apiId) {
        return ApiResponse.ok(apiKeywordService.generateFromApi(projectId, apiId));
    }
}
