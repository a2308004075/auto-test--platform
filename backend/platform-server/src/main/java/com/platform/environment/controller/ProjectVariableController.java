/**
 * @author HXN
 * @date 2026-08-24
 * @description 项目全局变量管理控制器
 */
package com.platform.environment.controller;

import com.platform.common.response.ApiResponse;
import com.platform.environment.dto.ProjectVariableDTO;
import com.platform.environment.service.ProjectVariableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 项目全局变量管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/variables")
@RequiredArgsConstructor
public class ProjectVariableController {

    private final ProjectVariableService projectVariableService;

    /**
     * 查询项目全局变量列表
     */
    @GetMapping
    public ApiResponse<List<ProjectVariableDTO>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(projectVariableService.list(projectId));
    }

    /**
     * 全量更新项目全局变量
     */
    @PostMapping
    public ApiResponse<Void> update(@PathVariable Long projectId,
                                    @Valid @RequestBody List<ProjectVariableDTO> variables) {
        projectVariableService.update(projectId, variables);
        return ApiResponse.ok();
    }
}
