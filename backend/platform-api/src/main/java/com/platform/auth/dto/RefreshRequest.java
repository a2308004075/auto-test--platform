/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Token 刷新请求 DTO
 */
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
