package com.postman.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 更新用户请求
 */
@Data
public class UserUpdateRequest {

    @Size(max = 50, message = "显示名长度不能超过 50")
    private String displayName;

    @Pattern(regexp = "^(ADMIN|USER)$", message = "角色必须为 ADMIN 或 USER")
    private String role;
}
