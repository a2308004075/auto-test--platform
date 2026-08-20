package com.postman.platform.auth.dto;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserBriefDTO user;
}
