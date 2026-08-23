/**
 * @author HXN
 * @date 2026-08-24
 * @description Action 关键字分组管理控制器
 */
package com.platform.action.controller;

import com.platform.action.dto.ActionGroupCreateRequest;
import com.platform.action.dto.ActionGroupResponse;
import com.platform.action.dto.ActionGroupUpdateRequest;
import com.platform.action.service.ActionGroupService;
import com.platform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Action 关键字分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/action-groups")
@RequiredArgsConstructor
public class ActionGroupController {

    private final ActionGroupService actionGroupService;

    /**
     * 查询分组列表
     */
    @GetMapping
    public ApiResponse<List<ActionGroupResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(actionGroupService.listByProject(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<ActionGroupResponse> create(@PathVariable Long projectId,
                                                   @Valid @RequestBody ActionGroupCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(actionGroupService.create(request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<ActionGroupResponse> update(@PathVariable Long projectId,
                                                   @PathVariable Long groupId,
                                                   @Valid @RequestBody ActionGroupUpdateRequest request) {
        return ApiResponse.ok(actionGroupService.update(groupId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                    @PathVariable Long groupId) {
        actionGroupService.delete(groupId);
        return ApiResponse.ok();
    }
}
