/**
 * @author HXN
 * @date 2026-09-14
 * @description 代码仓库分组管理控制器
 */
package com.platform.repository.controller;

import com.platform.common.response.ApiResponse;
import com.platform.repository.dto.RepositoryGroupCreateRequest;
import com.platform.repository.dto.RepositoryGroupResponse;
import com.platform.repository.dto.RepositoryGroupUpdateRequest;
import com.platform.repository.service.CodeRepositoryGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 代码仓库分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/repository-groups")
@RequiredArgsConstructor
public class CodeRepositoryGroupController {

    private final CodeRepositoryGroupService repositoryGroupService;

    /**
     * 查询分组列表
     */
    @GetMapping
    public ApiResponse<List<RepositoryGroupResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(repositoryGroupService.listByProject(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<RepositoryGroupResponse> create(@PathVariable Long projectId,
                                                       @Valid @RequestBody RepositoryGroupCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(repositoryGroupService.create(request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<RepositoryGroupResponse> update(@PathVariable Long projectId,
                                                       @PathVariable Long groupId,
                                                       @Valid @RequestBody RepositoryGroupUpdateRequest request) {
        return ApiResponse.ok(repositoryGroupService.update(groupId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                    @PathVariable Long groupId) {
        repositoryGroupService.delete(groupId);
        return ApiResponse.ok();
    }
}
