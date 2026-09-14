/**
 * @author HXN
 * @date 2026-09-14
 * @description 代码仓库分组响应 DTO
 */
package com.platform.repository.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓库分组响应
 */
@Data
public class RepositoryGroupResponse {

    private Long id;

    private Long projectId;

    private Long parentId;

    private String name;

    private String description;

    private Integer isSystem;

    /**
     * 分组下仓库数（含子孙分组，自底向上聚合）
     */
    private Integer repoCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
