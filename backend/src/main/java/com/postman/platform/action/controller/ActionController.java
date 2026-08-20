package com.postman.platform.action.controller;

import com.postman.platform.action.dto.*;
import com.postman.platform.action.service.ActionService;
import com.postman.platform.common.response.ApiResponse;
import com.postman.platform.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Action 关键字管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/actions")
@RequiredArgsConstructor
public class ActionController {

    private final ActionService actionService;

    @GetMapping
    public ApiResponse<PageResponse<ActionResponse>> list(
            @PathVariable String projectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(actionService.list(projectId, keyword, page, pageSize));
    }

    @PostMapping
    public ApiResponse<ActionResponse> create(@PathVariable String projectId,
                                                @Valid @RequestBody ActionCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(actionService.create(request));
    }

    @GetMapping("/{actionId}")
    public ApiResponse<ActionResponse> get(@PathVariable String projectId,
                                            @PathVariable String actionId) {
        return ApiResponse.ok(actionService.getById(actionId));
    }

    @PutMapping("/{actionId}")
    public ApiResponse<ActionResponse> update(@PathVariable String projectId,
                                                @PathVariable String actionId,
                                                @Valid @RequestBody ActionUpdateRequest request) {
        return ApiResponse.ok(actionService.update(actionId, request));
    }

    @DeleteMapping("/{actionId}")
    public ApiResponse<Void> delete(@PathVariable String projectId,
                                     @PathVariable String actionId) {
        actionService.delete(actionId);
        return ApiResponse.ok();
    }

    @PostMapping("/{actionId}/debug")
    public ApiResponse<ActionDebugResponse> debug(@PathVariable String projectId,
                                                    @PathVariable String actionId,
                                                    @Valid @RequestBody ActionDebugRequest request) {
        return ApiResponse.ok(actionService.debug(actionId, request));
    }

    @GetMapping("/{actionId}/references")
    public ApiResponse<List<Map<String, Object>>> references(@PathVariable String projectId,
                                                               @PathVariable String actionId) {
        return ApiResponse.ok(actionService.getReferences(actionId));
    }
}
