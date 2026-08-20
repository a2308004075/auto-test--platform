package com.postman.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 重置密码请求
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 128, message = "密码长度必须在 8-128 之间")
    private String newPassword;
}
