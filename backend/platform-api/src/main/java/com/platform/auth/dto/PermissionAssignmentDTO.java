/**
 * @author HXN
 * @date 2026-08-22
 * @description 权限分配项 DTO（含按角色控制模式）
 */
package com.platform.auth.dto;

import lombok.Data;

/**
 * 权限分配项（用于角色创建/编辑/分配权限时传递 permissionId + controlMode）
 *
 * <p>controlMode 含义（按角色）：
 * <ul>
 *     <li>MENU 类型：null（不适用控制模式）</li>
 *     <li>BUTTON 类型：enabled-显示可点击，disabled-显示禁点击</li>
 * </ul>
 */
@Data
public class PermissionAssignmentDTO {

    /**
     * 权限 ID
     */
    private Long permissionId;

    /**
     * 按钮控制模式（按角色）：enabled-显示可点击，disabled-显示禁点击。
     * MENU 类型为 null。
     */
    private String controlMode;
}
