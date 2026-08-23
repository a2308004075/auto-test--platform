/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 工具方法管理控制器
 */
package com.platform.tool.controller;

import com.platform.common.dto.ReferenceDetailResponse;
import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import com.platform.tool.dto.*;
import com.platform.tool.service.ToolMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
            @PathVariable Long projectId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(toolMethodService.list(projectId, category, keyword, page, pageSize));
    }

    @PostMapping
    public ApiResponse<ToolMethodResponse> create(@PathVariable Long projectId,
                                                    @Valid @RequestBody ToolMethodCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(toolMethodService.create(request));
    }

    @GetMapping("/{toolId}")
    public ApiResponse<ToolMethodResponse> get(@PathVariable Long projectId,
                                                @PathVariable Long toolId) {
        return ApiResponse.ok(toolMethodService.getById(toolId));
    }

    @PostMapping("/{toolId}")
    public ApiResponse<ToolMethodResponse> update(@PathVariable Long projectId,
                                                    @PathVariable Long toolId,
                                                    @Valid @RequestBody ToolMethodUpdateRequest request) {
        return ApiResponse.ok(toolMethodService.update(toolId, request));
    }

    @PostMapping("/{toolId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long toolId) {
        toolMethodService.delete(toolId);
        return ApiResponse.ok();
    }

    @PostMapping("/{toolId}/test")
    public ApiResponse<ToolTestResult> test(@PathVariable Long projectId,
                                             @PathVariable Long toolId,
                                             @Valid @RequestBody ToolTestRequest request) {
        return ApiResponse.ok(toolMethodService.testTool(toolId, request));
    }

    /**
     * 查询工具方法引用关系详情
     */
    @GetMapping("/{toolId}/dependencies")
    public ApiResponse<List<ReferenceDetailResponse>> getDependencies(@PathVariable Long projectId,
                                                                       @PathVariable Long toolId) {
        return ApiResponse.ok(toolMethodService.getDependencies(toolId));
    }
}
