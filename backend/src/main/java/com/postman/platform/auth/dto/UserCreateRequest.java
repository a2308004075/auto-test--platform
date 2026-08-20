package com.postman.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 创建用户请求
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 50, message = "账号长度不能超过 50")
    private String username;

    @NotBlank(message = "显示名不能为空")
    @Size(max = 50, message = "显示名长度不能超过 50")
    private String displayName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 128, message = "密码长度必须在 8-128 之间")
    private String password;

    @Pattern(regexp = "^(ADMIN|USER)$", message = "角色必须为 ADMIN 或 USER")
    private String role = "USER";
}
