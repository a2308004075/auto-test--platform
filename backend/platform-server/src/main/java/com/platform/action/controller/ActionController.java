/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 关键字管理控制器
 */
package com.platform.action.controller;

import com.platform.action.dto.*;
import com.platform.action.service.ActionService;
import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
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
            @PathVariable Long projectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(actionService.list(projectId, keyword, groupId, page, pageSize));
    }

    @PostMapping
    public ApiResponse<ActionResponse> create(@PathVariable Long projectId,
                                                @Valid @RequestBody ActionCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(actionService.create(request));
    }

    @GetMapping("/{actionId}")
    public ApiResponse<ActionResponse> get(@PathVariable Long projectId,
                                            @PathVariable Long actionId) {
        return ApiResponse.ok(actionService.getById(actionId));
    }

    @PostMapping("/{actionId}")
    public ApiResponse<ActionResponse> update(@PathVariable Long projectId,
                                                @PathVariable Long actionId,
                                                @Valid @RequestBody ActionUpdateRequest request) {
        return ApiResponse.ok(actionService.update(actionId, request));
    }

    @PostMapping("/{actionId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long actionId) {
        actionService.delete(actionId);
        return ApiResponse.ok();
    }

    @PostMapping("/batch-move")
    public ApiResponse<Void> batchMove(@PathVariable Long projectId,
                                        @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> actionIds = (List<Number>) body.get("actionIds");
        Long targetGroupId = body.get("targetGroupId") != null
                ? ((Number) body.get("targetGroupId")).longValue() : null;
        List<Long> ids = actionIds.stream().map(Number::longValue).collect(java.util.stream.Collectors.toList());
        actionService.batchMove(projectId, ids, targetGroupId);
        return ApiResponse.ok();
    }

    @PostMapping("/{actionId}/debug")
    public ApiResponse<ActionDebugResponse> debug(@PathVariable Long projectId,
                                                    @PathVariable Long actionId,
                                                    @Valid @RequestBody ActionDebugRequest request) {
        return ApiResponse.ok(actionService.debug(actionId, request));
    }

    @GetMapping("/{actionId}/references")
    public ApiResponse<List<Map<String, Object>>> references(@PathVariable Long projectId,
                                                               @PathVariable Long actionId) {
        return ApiResponse.ok(actionService.getReferences(actionId));
    }
}
