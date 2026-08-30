/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 套件分组管理控制器
 */
package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.execution.dto.SuiteGroupDTO;
import com.platform.execution.dto.SuiteGroupRequest;
import com.platform.execution.service.SuiteGroupService;
import com.platform.execution.service.SuiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 套件分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/suite-groups")
@RequiredArgsConstructor
public class SuiteGroupController {

    private final SuiteGroupService suiteGroupService;
    private final SuiteService suiteService;

    /**
     * 查询项目下所有分组
     */
    @GetMapping
    public ApiResponse<List<SuiteGroupDTO>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(suiteGroupService.listGroups(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<SuiteGroupDTO> create(@PathVariable Long projectId,
                                              @Valid @RequestBody SuiteGroupRequest request) {
        return ApiResponse.ok(suiteGroupService.createGroup(projectId, request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<SuiteGroupDTO> update(@PathVariable Long projectId,
                                              @PathVariable Long groupId,
                                              @Valid @RequestBody SuiteGroupRequest request) {
        return ApiResponse.ok(suiteGroupService.updateGroup(projectId, groupId, request));
    }

    /**
     * 删除分组（该分组下的套件自动归入未分组）
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long groupId) {
        suiteGroupService.deleteGroup(projectId, groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空分组及其子孙分组中的所有套件
     */
    @PostMapping("/{groupId}/clear-suites")
    public ApiResponse<Void> clearSuites(@PathVariable Long projectId,
                                         @PathVariable Long groupId) {
        suiteService.clearByGroup(projectId, groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空项目下所有套件
     */
    @PostMapping("/clear-all-suites")
    public ApiResponse<Void> clearAllSuites(@PathVariable Long projectId) {
        suiteService.clearByProject(projectId);
        return ApiResponse.ok();
    }
}
