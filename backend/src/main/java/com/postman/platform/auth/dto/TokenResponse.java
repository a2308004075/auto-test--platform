package com.postman.platform.auth.dto;

import lombok.Data;

/**
 * Token 刷新响应
 */
@Data
public class TokenResponse {

    private String accessToken;
    private Long expiresIn;
}
