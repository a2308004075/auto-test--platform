/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档分组更新请求 DTO
 */
package com.platform.projectdoc.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 更新项目文档分组请求
 */
@Data
public class ProjectDocGroupUpdateRequest {

    /**
     * 父分组 ID（null=不修改）
     */
    private Long parentId;

    @Size(max = 100, message = "分组名称长度不能超过 100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;
}
