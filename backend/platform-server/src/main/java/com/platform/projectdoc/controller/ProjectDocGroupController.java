/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档分组管理控制器
 */
package com.platform.projectdoc.controller;

import com.platform.common.response.ApiResponse;
import com.platform.projectdoc.dto.ProjectDocGroupCreateRequest;
import com.platform.projectdoc.dto.ProjectDocGroupResponse;
import com.platform.projectdoc.dto.ProjectDocGroupUpdateRequest;
import com.platform.projectdoc.service.ProjectDocGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 项目文档分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/doc-groups")
@RequiredArgsConstructor
public class ProjectDocGroupController {

    private final ProjectDocGroupService projectDocGroupService;

    /**
     * 查询分组列表
     */
    @GetMapping
    public ApiResponse<List<ProjectDocGroupResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(projectDocGroupService.listByProject(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<ProjectDocGroupResponse> create(@PathVariable Long projectId,
                                                       @Valid @RequestBody ProjectDocGroupCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(projectDocGroupService.create(request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<ProjectDocGroupResponse> update(@PathVariable Long projectId,
                                                       @PathVariable Long groupId,
                                                       @Valid @RequestBody ProjectDocGroupUpdateRequest request) {
        return ApiResponse.ok(projectDocGroupService.update(groupId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                    @PathVariable Long groupId) {
        projectDocGroupService.delete(groupId);
        return ApiResponse.ok();
    }
}
