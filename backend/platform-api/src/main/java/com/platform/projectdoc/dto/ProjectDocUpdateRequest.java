/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档更新请求 DTO
 */
package com.platform.projectdoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 更新项目文档请求（重命名/描述/移动分组）
 */
@Data
public class ProjectDocUpdateRequest {

    @NotBlank(message = "文档名称不能为空")
    @Size(max = 200, message = "文档名称长度不能超过 200")
    private String docName;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;

    /**
     * 目标分组 ID（null=不修改，0=移入未分组，正数=目标分组）
     */
    private Long groupId;
}
