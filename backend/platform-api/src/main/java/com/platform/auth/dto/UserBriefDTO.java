/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 用户简要信息 DTO
 */
package com.platform.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户简要信息（嵌套在登录响应中）
 */
@Data
public class UserBriefDTO {

    private Long id;
    private String username;
    private String displayName;
    private String role;

    /**
     * 权限编码列表（ADMIN 返回 ["*"]）
     */
    private List<String> permissions;

    /**
     * 权限详情列表（含控制模式，供前端 v-permission 指令使用）
     */
    private List<PermissionBriefDTO> permissionDetails;
}
