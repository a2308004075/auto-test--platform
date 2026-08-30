/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 测试代码仓库管理控制器
 */
package com.platform.repository.controller;

import com.platform.common.response.ApiResponse;
import com.platform.repository.dto.PullLogResponse;
import com.platform.repository.dto.PullResultResponse;
import com.platform.repository.dto.RepositoryCreateRequest;
import com.platform.repository.dto.RepositoryResponse;
import com.platform.repository.dto.RepositoryUpdateRequest;
import com.platform.repository.service.CodeRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 测试代码仓库管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/repositories")
@RequiredArgsConstructor
public class CodeRepositoryController {

    private final CodeRepositoryService repositoryService;

    /**
     * 查询项目下的仓库列表
     */
    @GetMapping
    public ApiResponse<List<RepositoryResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(repositoryService.listByProject(projectId));
    }

    /**
     * 创建仓库
     */
    @PostMapping
    public ApiResponse<RepositoryResponse> create(@PathVariable Long projectId,
                                                  @Valid @RequestBody RepositoryCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(repositoryService.create(request));
    }

    /**
     * 更新仓库
     */
    @PostMapping("/{repoId}")
    public ApiResponse<RepositoryResponse> update(@PathVariable Long projectId,
                                                  @PathVariable Long repoId,
                                                  @Valid @RequestBody RepositoryUpdateRequest request) {
        return ApiResponse.ok(repositoryService.update(repoId, request));
    }

    /**
     * 删除仓库（同时删除本地代码目录）
     */
    @PostMapping("/{repoId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                    @PathVariable Long repoId) {
        repositoryService.delete(repoId);
        return ApiResponse.ok();
    }

    /**
     * 拉取仓库代码（同步执行，返回拉取结果）
     */
    @PostMapping("/{repoId}/pull")
    public ApiResponse<PullResultResponse> pull(@PathVariable Long projectId,
                                                @PathVariable Long repoId) {
        return ApiResponse.ok(repositoryService.pull(repoId));
    }

    /**
     * 查询仓库拉取历史（最近 20 条）
     */
    @GetMapping("/{repoId}/pull-logs")
    public ApiResponse<List<PullLogResponse>> listPullLogs(@PathVariable Long projectId,
                                                           @PathVariable Long repoId) {
        return ApiResponse.ok(repositoryService.listPullLogs(repoId));
    }
}
