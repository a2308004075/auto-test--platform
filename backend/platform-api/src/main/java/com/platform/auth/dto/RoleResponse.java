/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description 角色响应 DTO
 */
package com.platform.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色响应
 */
@Data
public class RoleResponse {

    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    private Integer sortOrder;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 已分配的权限列表（含按角色控制模式）
     */
    private List<PermissionAssignmentDTO> permissions;
}
