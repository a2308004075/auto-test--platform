package com.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 更新用户请求
 */
@Data
public class UserUpdateRequest {

    @Size(max = 50, message = "显示名长度不能超过 50")
    private String displayName;

    private Long roleId;
}
