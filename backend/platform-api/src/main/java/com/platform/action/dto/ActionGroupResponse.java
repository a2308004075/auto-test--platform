/**
 * @author HXN
 * @date 2026-08-24
 * @description Action 分组响应 DTO
 */
package com.platform.action.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Action 分组响应
 */
@Data
public class ActionGroupResponse {

    private Long id;
    private Long projectId;
    private Long parentId;
    private String name;
    private String description;
    private Integer isSystem;
    private Integer actionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
