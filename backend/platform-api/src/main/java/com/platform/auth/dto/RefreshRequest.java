package com.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Token 刷新请求
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
