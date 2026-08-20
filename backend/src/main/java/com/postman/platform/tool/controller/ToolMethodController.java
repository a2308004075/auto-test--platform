package com.postman.platform.tool.controller;

import com.postman.platform.common.response.ApiResponse;
import com.postman.platform.common.response.PageResponse;
import com.postman.platform.tool.dto.*;
import com.postman.platform.tool.service.ToolMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 工具方法管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/tools")
@RequiredArgsConstructor
public class ToolMethodController {

    private final ToolMethodService toolMethodService;

    @GetMapping
    public ApiResponse<PageResponse<ToolMethodResponse>> list(
            @PathVariable String projectId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(toolMethodService.list(projectId, category, keyword, page, pageSize));
    }

    @PostMapping
    public ApiResponse<ToolMethodResponse> create(@PathVariable String projectId,
                                                    @Valid @RequestBody ToolMethodCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(toolMethodService.create(request));
    }

    @GetMapping("/{toolId}")
    public ApiResponse<ToolMethodResponse> get(@PathVariable String projectId,
                                                @PathVariable String toolId) {
        return ApiResponse.ok(toolMethodService.getById(toolId));
    }

    @PutMapping("/{toolId}")
    public ApiResponse<ToolMethodResponse> update(@PathVariable String projectId,
                                                    @PathVariable String toolId,
                                                    @Valid @RequestBody ToolMethodUpdateRequest request) {
        return ApiResponse.ok(toolMethodService.update(toolId, request));
    }

    @DeleteMapping("/{toolId}")
    public ApiResponse<Void> delete(@PathVariable String projectId,
                                     @PathVariable String toolId) {
        toolMethodService.delete(toolId);
        return ApiResponse.ok();
    }

    @PostMapping("/{toolId}/test")
    public ApiResponse<ToolTestResult> test(@PathVariable String projectId,
                                             @PathVariable String toolId,
                                             @Valid @RequestBody ToolTestRequest request) {
        return ApiResponse.ok(toolMethodService.testTool(toolId, request));
    }
}
