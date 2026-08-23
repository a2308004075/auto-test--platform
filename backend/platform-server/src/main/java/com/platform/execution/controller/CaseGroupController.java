/**
 * @author HXN
 * @date 2026-08-23
 * @description 测试用例分组管理控制器
 */
package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.execution.dto.CaseGroupCreateRequest;
import com.platform.execution.dto.CaseGroupResponse;
import com.platform.execution.dto.CaseGroupUpdateRequest;
import com.platform.execution.service.CaseGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 测试用例分组管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/case-groups")
@RequiredArgsConstructor
public class CaseGroupController {

    private final CaseGroupService caseGroupService;

    /**
     * 查询分组列表
     */
    @GetMapping
    public ApiResponse<List<CaseGroupResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(caseGroupService.listByProject(projectId));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public ApiResponse<CaseGroupResponse> create(@PathVariable Long projectId,
                                                  @Valid @RequestBody CaseGroupCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(caseGroupService.create(request));
    }

    /**
     * 更新分组
     */
    @PostMapping("/{groupId}")
    public ApiResponse<CaseGroupResponse> update(@PathVariable Long projectId,
                                                  @PathVariable Long groupId,
                                                  @Valid @RequestBody CaseGroupUpdateRequest request) {
        return ApiResponse.ok(caseGroupService.update(groupId, request));
    }

    /**
     * 删除分组
     */
    @PostMapping("/{groupId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long projectId,
                                     @PathVariable Long groupId) {
        caseGroupService.delete(groupId);
        return ApiResponse.ok();
    }
}
