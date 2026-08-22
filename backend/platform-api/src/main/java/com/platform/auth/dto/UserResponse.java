/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 用户响应 DTO
 */
package com.platform.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应
 */
@Data
public class UserResponse {

    private Long id;
    private String username;
    private String displayName;
    /**
     * 个人简介（选填）
     */
    private String bio;
    private Long roleId;
    private String roleName;
    /**
     * 角色编码（如 ADMIN、TESTER），供前端兼容展示
     */
    private String role;
    private Integer isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    /**
     * 权限编码列表（ADMIN 返回 ["*"]）
     */
    private List<String> permissions;
}
