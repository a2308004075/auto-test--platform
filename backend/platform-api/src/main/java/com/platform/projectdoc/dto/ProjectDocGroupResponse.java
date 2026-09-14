/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档分组响应 DTO
 */
package com.platform.projectdoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目文档分组响应
 */
@Data
public class ProjectDocGroupResponse {

    private Long id;
    private Long projectId;
    private Long parentId;
    private String name;
    private String description;
    private Integer isSystem;

    /**
     * 分组下的文档数（含子孙分组，自底向上聚合）
     */
    private Integer docCount;

    private LocalDateTime createdAt;
}
