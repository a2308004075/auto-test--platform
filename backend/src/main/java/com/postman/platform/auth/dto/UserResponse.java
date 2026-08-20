package com.postman.platform.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应
 */
@Data
public class UserResponse {

    private String id;
    private String username;
    private String displayName;
    private String roleId;
    private String roleName;
    /**
     * 角色编码（如 ADMIN、USER），供前端兼容展示
     */
    private String role;
    private Boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
