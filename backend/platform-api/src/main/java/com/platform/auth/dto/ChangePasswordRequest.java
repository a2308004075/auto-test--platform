/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 修改密码请求 DTO
 */
package com.platform.auth.dto;

import com.platform.common.constant.PasswordPolicy;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 修改密码请求（当前用户修改自己的密码）
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = PasswordPolicy.MIN_LENGTH, message = PasswordPolicy.SIZE_MESSAGE)
    @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.PATTERN_MESSAGE)
    private String newPassword;
}
