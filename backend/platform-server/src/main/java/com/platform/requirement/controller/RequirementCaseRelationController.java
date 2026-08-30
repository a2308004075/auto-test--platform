/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求-用例关联管理控制器
 */
package com.platform.requirement.controller;

import com.platform.common.response.ApiResponse;
import com.platform.requirement.dto.RequirementCaseRelationCreateRequest;
import com.platform.requirement.dto.RequirementCaseRelationResponse;
import com.platform.requirement.service.RequirementCaseRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 需求-用例关联管理接口
 *
 * <p>需求条目视角：/api/v1/requirement-items/{itemId}/case-relations
 * <p>用例视角（反查）：/api/v1/projects/{projectId}/case-requirement-relations
 */
@RestController
@RequiredArgsConstructor
public class RequirementCaseRelationController {

    private final RequirementCaseRelationService relationService;

    /**
     * 查询需求条目下关联的用例列表
     */
    @GetMapping("/api/v1/requirement-items/{itemId}/case-relations")
    public ApiResponse<List<RequirementCaseRelationResponse>> listByItem(@PathVariable Long itemId) {
        return ApiResponse.ok(relationService.listByItem(itemId));
    }

    /**
     * 添加需求条目与用例的关联
     */
    @PostMapping("/api/v1/requirement-items/{itemId}/case-relations")
    public ApiResponse<RequirementCaseRelationResponse> addRelation(@PathVariable Long itemId,
                                                                    @Valid @RequestBody RequirementCaseRelationCreateRequest request) {
        return ApiResponse.ok(relationService.addRelation(itemId, request));
    }

    /**
     * 查询用例关联的需求条目列表（反查）
     */
    @GetMapping("/api/v1/projects/{projectId}/case-requirement-relations")
    public ApiResponse<List<RequirementCaseRelationResponse>> listByCase(@PathVariable Long projectId,
                                                                         @RequestParam String caseType,
                                                                         @RequestParam Long caseId) {
        return ApiResponse.ok(relationService.listByCase(projectId, caseType, caseId));
    }

    /**
     * 删除需求-用例关联
     */
    @PostMapping("/api/v1/requirement-case-relations/{relationId}/delete")
    public ApiResponse<Void> deleteRelation(@PathVariable Long relationId) {
        relationService.deleteRelation(relationId);
        return ApiResponse.ok();
    }
}
