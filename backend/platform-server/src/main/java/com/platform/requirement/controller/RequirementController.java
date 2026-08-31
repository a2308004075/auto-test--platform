/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求文档管理控制器
 */
package com.platform.requirement.controller;

import com.platform.common.response.ApiResponse;
import com.platform.requirement.dto.RequirementItemCreateRequest;
import com.platform.requirement.dto.RequirementItemResponse;
import com.platform.requirement.dto.RequirementVersionCreateRequest;
import com.platform.requirement.dto.RequirementVersionResponse;
import com.platform.requirement.service.RequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 需求文档管理接口
 *
 * <p>版本接口：/api/v1/projects/{projectId}/requirement-versions
 * <p>条目接口：/api/v1/requirement-versions/{versionId}/items
 */
@RestController
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;

    // ===== 版本管理 =====

    /**
     * 查询项目下的版本列表
     */
    @GetMapping("/api/v1/projects/{projectId}/requirement-versions")
    public ApiResponse<List<RequirementVersionResponse>> listVersions(@PathVariable Long projectId) {
        return ApiResponse.ok(requirementService.listVersions(projectId));
    }

    /**
     * 创建版本
     */
    @PostMapping("/api/v1/projects/{projectId}/requirement-versions")
    public ApiResponse<RequirementVersionResponse> createVersion(@PathVariable Long projectId,
                                                                  @Valid @RequestBody RequirementVersionCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(requirementService.createVersion(request));
    }

    /**
     * 更新版本
     */
    @PostMapping("/api/v1/requirement-versions/{versionId}")
    public ApiResponse<RequirementVersionResponse> updateVersion(@PathVariable Long versionId,
                                                                  @Valid @RequestBody RequirementVersionCreateRequest request) {
        return ApiResponse.ok(requirementService.updateVersion(versionId, request));
    }

    /**
     * 删除版本（级联删除其下所有条目）
     */
    @PostMapping("/api/v1/requirement-versions/{versionId}/delete")
    public ApiResponse<Void> deleteVersion(@PathVariable Long versionId) {
        requirementService.deleteVersion(versionId);
        return ApiResponse.ok();
    }

    // ===== 需求条目管理 =====

    /**
     * 查询单个需求条目详情
     */
    @GetMapping("/api/v1/requirement-items/{itemId}")
    public ApiResponse<RequirementItemResponse> getItem(@PathVariable Long itemId) {
        return ApiResponse.ok(requirementService.getItem(itemId));
    }

    /**
     * 查询版本下的需求条目列表
     */
    @GetMapping("/api/v1/requirement-versions/{versionId}/items")
    public ApiResponse<List<RequirementItemResponse>> listItems(@PathVariable Long versionId) {
        return ApiResponse.ok(requirementService.listItems(versionId));
    }

    /**
     * 创建需求条目
     */
    @PostMapping("/api/v1/requirement-versions/{versionId}/items")
    public ApiResponse<RequirementItemResponse> createItem(@PathVariable Long versionId,
                                                            @Valid @RequestBody RequirementItemCreateRequest request) {
        request.setVersionId(versionId);
        return ApiResponse.ok(requirementService.createItem(request));
    }

    /**
     * 更新需求条目
     */
    @PostMapping("/api/v1/requirement-items/{itemId}")
    public ApiResponse<RequirementItemResponse> updateItem(@PathVariable Long itemId,
                                                            @Valid @RequestBody RequirementItemCreateRequest request) {
        return ApiResponse.ok(requirementService.updateItem(itemId, request));
    }

    /**
     * 删除需求条目
     */
    @PostMapping("/api/v1/requirement-items/{itemId}/delete")
    public ApiResponse<Void> deleteItem(@PathVariable Long itemId) {
        requirementService.deleteItem(itemId);
        return ApiResponse.ok();
    }
}
