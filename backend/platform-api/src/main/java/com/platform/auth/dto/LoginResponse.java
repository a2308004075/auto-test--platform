/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 登录响应 DTO
 */
package com.platform.auth.dto;

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
