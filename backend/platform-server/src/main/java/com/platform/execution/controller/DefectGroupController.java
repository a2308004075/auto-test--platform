/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷分组管理控制器
 */
package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.execution.dto.DefectGroupCreateRequest;
import com.platform.execution.dto.DefectGroupResponse;
import com.platform.execution.dto.DefectGroupUpdateRequest;
import com.platform.execution.service.DefectGroupService;
import com.platform.execution.service.DefectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 缺陷分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/defect-groups")
@RequiredArgsConstructor
public class DefectGroupController {

    private final DefectGroupService defectGroupService;
    private final DefectService defectService;

    /**
     * 查询分组列表
     */
    @GetMapping
    public ApiResponse<List<DefectGroupResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(defectGroupService.listByProject(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<DefectGroupResponse> create(@PathVariable Long projectId,
                                                    @Valid @RequestBody DefectGroupCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(defectGroupService.create(request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<DefectGroupResponse> update(@PathVariable Long projectId,
                                                    @PathVariable Long groupId,
                                                    @Valid @RequestBody DefectGroupUpdateRequest request) {
        return ApiResponse.ok(defectGroupService.update(groupId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long groupId) {
        defectGroupService.delete(groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空分组及其子孙分组中的所有缺陷
     */
    @PostMapping("/{groupId}/clear-defects")
    public ApiResponse<Void> clearDefects(@PathVariable Long projectId,
                                           @PathVariable Long groupId) {
        defectService.clearByGroup(projectId, groupId);
        return ApiResponse.ok();
    }

    /**
     * 清空项目下所有缺陷
     */
    @PostMapping("/clear-all-defects")
    public ApiResponse<Void> clearAllDefects(@PathVariable Long projectId) {
        defectService.clearByProject(projectId);
        return ApiResponse.ok();
    }
}
