package com.platform.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应
 */
@Data
public class UserResponse {

    private Long id;
    private String username;
    private String displayName;
    private Long roleId;
    private String roleName;
    /**
     * 角色编码（如 ADMIN、TESTER），供前端兼容展示
     */
    private String role;
    private Integer isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
