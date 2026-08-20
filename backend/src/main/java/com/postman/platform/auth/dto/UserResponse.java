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
    private String role;
    private Boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
