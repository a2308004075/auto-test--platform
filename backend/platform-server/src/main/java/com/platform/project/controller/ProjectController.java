package com.platform.project.controller;

import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import com.platform.project.dto.*;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 项目管理接口
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 分页查询项目列表
     */
    @GetMapping
    public ApiResponse<PageResponse<ProjectResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(projectService.listProjects(keyword, status, page, pageSize));
    }

    /**
     * 创建项目
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.ok(projectService.createProject(request));
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> get(@PathVariable Long projectId) {
        return ApiResponse.ok(projectService.getProject(projectId));
    }

    /**
     * 更新项目
     */
    @PutMapping("/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProjectResponse> update(@PathVariable Long projectId,
                                                @Valid @RequestBody ProjectUpdateRequest request) {
        return ApiResponse.ok(projectService.updateProject(projectId, request));
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ApiResponse.ok();
    }

    /**
     * 启停项目
     */
    @PatchMapping("/{projectId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProjectResponse> toggleStatus(@PathVariable Long projectId) {
        return ApiResponse.ok(projectService.toggleStatus(projectId));
    }

    /**
     * 获取项目仪表板
     */
    @GetMapping("/{projectId}/dashboard")
    public ApiResponse<ProjectDashboardResponse> dashboard(@PathVariable Long projectId) {
        return ApiResponse.ok(projectService.getDashboard(projectId));
    }
}
