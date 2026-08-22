/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 用户创建请求 DTO
 */
package com.platform.auth.dto;

import com.platform.common.constant.PasswordPolicy;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 创建用户请求
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 6, max = 50, message = "账号长度必须在 6-50 之间")
    private String username;

    @NotBlank(message = "显示名不能为空")
    @Size(max = 50, message = "显示名长度不能超过 50")
    private String displayName;

    @NotBlank(message = "密码不能为空")
    @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH, message = PasswordPolicy.SIZE_MESSAGE)
    @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.PATTERN_MESSAGE)
    private String password;

    @NotNull(message = "角色 ID 不能为空")
    private Long roleId;
}
