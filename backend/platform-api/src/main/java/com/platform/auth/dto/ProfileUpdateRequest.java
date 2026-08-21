package com.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 个人资料更新请求（当前用户修改自己的资料）
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 50, message = "显示名长度不能超过 50")
    private String displayName;

    @Size(max = 50, message = "账号长度不能超过 50")
    private String username;
}
