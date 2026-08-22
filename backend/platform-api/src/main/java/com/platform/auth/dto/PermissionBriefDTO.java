/**
 * @author HXN
 * @date 2026-08-22
 * @description 权限简要信息 DTO（含控制模式）
 */
package com.platform.auth.dto;

import lombok.Data;

/**
 * 权限简要信息（嵌入登录响应和用户响应中，供前端 v-permission 指令使用）
 */
@Data
public class PermissionBriefDTO {

    /**
     * 权限编码（如 system:user:add）
     */
    private String code;

    /**
     * 权限类型：MENU / BUTTON
     */
    private String type;

    /**
     * 按钮控制模式：display-无权限时隐藏，click-无权限时禁用
     * MENU 类型此字段为 null
     */
    private String controlMode;
}
