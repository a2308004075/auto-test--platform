package com.platform.auth.dto;

import lombok.Data;

/**
 * 用户简要信息（嵌套在登录响应中）
 */
@Data
public class UserBriefDTO {

    private Long id;
    private String username;
    private String displayName;
    private String role;
}
