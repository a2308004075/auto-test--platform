/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 自动化套件分组管理控制器
 */
package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.execution.dto.AutoSuiteGroupDTO;
import com.platform.execution.dto.AutoSuiteGroupRequest;
import com.platform.execution.service.AutoSuiteGroupService;
import com.platform.execution.service.AutoSuiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 自动化套件分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/auto-suite-groups")
@RequiredArgsConstructor
public class AutoSuiteGroupController {

    private final AutoSuiteGroupService autoSuiteGroupService;
    private final AutoSuiteService autoSuiteService;

    /**
     * 查询项目下所有分组
     */
    @GetMapping
    public ApiResponse<List<AutoSuiteGroupDTO>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(autoSuiteGroupService.listGroups(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<AutoSuiteGroupDTO> create(@PathVariable Long projectId,
                                                 @Valid @RequestBody AutoSuiteGroupRequest request) {
        return ApiResponse.ok(autoSuiteGroupService.createGroup(projectId, request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<AutoSuiteGroupDTO> update(@PathVariable Long projectId,
                                                 @PathVariable Long groupId,
                                                 @Valid @RequestBody AutoSuiteGroupRequest request) {
        return ApiResponse.ok(autoSuiteGroupService.updateGroup(projectId, groupId, request));
    }

    /**
     * 删除分组（该分组下的自动化套件自动归入未分组）
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                    @PathVariable Long groupId) {
        autoSuiteGroupService.deleteGroup(projectId, groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空分组及其子孙分组中的所有自动化套件
     */
    @PostMapping("/{groupId}/clear-suites")
    public ApiResponse<Void> clearSuites(@PathVariable Long projectId,
                                         @PathVariable Long groupId) {
        autoSuiteService.clearByGroup(projectId, groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空项目下所有自动化套件
     */
    @PostMapping("/clear-all-suites")
    public ApiResponse<Void> clearAllSuites(@PathVariable Long projectId) {
        autoSuiteService.clearByProject(projectId);
        return ApiResponse.ok();
    }
}
