/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Token 响应 DTO
 */
package com.platform.auth.dto;

import lombok.Data;

/**
 * Token 刷新响应
 */
@Data
public class TokenResponse {

    private String accessToken;
    private Long expiresIn;
}
