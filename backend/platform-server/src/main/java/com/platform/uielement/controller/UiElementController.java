/**
 * @author HXN
 * @date 2026-08-30 14:00
 * @description 界面元素管理控制器
 */
package com.platform.uielement.controller;

import com.platform.common.response.ApiResponse;
import com.platform.uielement.dto.UiElementFileDeleteRequest;
import com.platform.uielement.dto.UiElementFileNode;
import com.platform.uielement.dto.UiElementImportRequest;
import com.platform.uielement.dto.UiElementImportResponse;
import com.platform.uielement.dto.UiElementResponse;
import com.platform.uielement.service.UiElementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 界面元素管理接口
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/ui-elements")
@RequiredArgsConstructor
public class UiElementController {

    private final UiElementService uiElementService;

    /**
     * 查询项目下的界面元素文件树（仓库 → 目录 → 文件）
     */
    @GetMapping("/files")
    public ApiResponse<List<UiElementFileNode>> listFiles(@PathVariable Long projectId) {
        return ApiResponse.ok(uiElementService.listFileTree(projectId));
    }

    /**
     * 查询指定文件的界面元素列表
     */
    @GetMapping
    public ApiResponse<List<UiElementResponse>> list(@PathVariable Long projectId,
                                                     @RequestParam Long repositoryId,
                                                     @RequestParam String filePath) {
        return ApiResponse.ok(uiElementService.listElements(projectId, repositoryId, filePath));
    }

    /**
     * 导入界面元素（从已拉取仓库解析前端源码，覆盖式重建）
     */
    @PostMapping("/import")
    public ApiResponse<UiElementImportResponse> importElements(@PathVariable Long projectId,
                                                               @Valid @RequestBody UiElementImportRequest request) {
        return ApiResponse.ok(uiElementService.importFromRepository(projectId, request.getRepositoryId()));
    }

    /**
     * 删除指定文件的界面元素
     */
    @PostMapping("/file/delete")
    public ApiResponse<Void> deleteFile(@PathVariable Long projectId,
                                        @Valid @RequestBody UiElementFileDeleteRequest request) {
        uiElementService.deleteByFile(projectId, request);
        return ApiResponse.ok();
    }

    /**
     * 删除仓库的全部界面元素
     */
    @PostMapping("/repository/{repositoryId}/delete")
    public ApiResponse<Void> deleteRepository(@PathVariable Long projectId,
                                              @PathVariable Long repositoryId) {
        uiElementService.deleteByRepository(projectId, repositoryId);
        return ApiResponse.ok();
    }
}
