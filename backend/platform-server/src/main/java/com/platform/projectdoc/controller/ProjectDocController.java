/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档管理控制器
 */
package com.platform.projectdoc.controller;

import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import com.platform.projectdoc.dto.ProjectDocResponse;
import com.platform.projectdoc.dto.ProjectDocUpdateRequest;
import com.platform.projectdoc.service.ProjectDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 项目文档管理接口
 *
 * <p>分组相关操作归属 ProjectDocGroupController。
 */
@RestController
@RequiredArgsConstructor
public class ProjectDocController {

    private final ProjectDocService projectDocService;

    /**
     * 分页查询项目文档列表
     */
    @GetMapping("/api/v1/projects/{projectId}/docs")
    public ApiResponse<PageResponse<ProjectDocResponse>> list(@PathVariable Long projectId,
                                                              @RequestParam(required = false) Long groupId,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(projectDocService.list(projectId, groupId, keyword, page, pageSize));
    }

    /**
     * 上传文档（multipart）
     */
    @PostMapping("/api/v1/projects/{projectId}/docs/upload")
    public ApiResponse<ProjectDocResponse> upload(@PathVariable Long projectId,
                                                  @RequestParam(required = false) Long groupId,
                                                  @RequestParam(required = false) String docName,
                                                  @RequestParam(required = false) String description,
                                                  @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(projectDocService.upload(projectId, groupId, docName, description, file));
    }

    /**
     * 更新文档（重命名/描述/移动分组）
     */
    @PostMapping("/api/v1/docs/{docId}")
    public ApiResponse<ProjectDocResponse> update(@PathVariable Long docId,
                                                  @Valid @RequestBody ProjectDocUpdateRequest request) {
        return ApiResponse.ok(projectDocService.update(docId, request));
    }

    /**
     * 替换文档文件（multipart）
     */
    @PostMapping("/api/v1/docs/{docId}/replace")
    public ApiResponse<ProjectDocResponse> replace(@PathVariable Long docId,
                                                   @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(projectDocService.replace(docId, file));
    }

    /**
     * 删除文档
     */
    @PostMapping("/api/v1/docs/{docId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long docId) {
        projectDocService.delete(docId);
        return ApiResponse.ok();
    }

    /**
     * 下载文档（文件流，不走 ApiResponse）
     */
    @GetMapping("/api/v1/docs/{docId}/download")
    public void download(@PathVariable Long docId, HttpServletResponse response) {
        projectDocService.download(docId, response);
    }
}
