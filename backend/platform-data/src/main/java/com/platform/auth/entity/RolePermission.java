/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description 角色权限关联实体类
 */
package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色-权限关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role_permission")
public class RolePermission extends BaseEntity {

    /**
     * 角色 ID（关联 user_role.id）
     */
    private Long roleId;

    /**
     * 权限 ID（关联 permission.id）
     */
    private Long permissionId;

    /**
     * 按钮控制模式（按角色）：
     * enabled-显示可点击，disabled-显示禁点击。
     * 仅 BUTTON 类型使用，MENU 类型为 null。
     */
    private String controlMode;
}
