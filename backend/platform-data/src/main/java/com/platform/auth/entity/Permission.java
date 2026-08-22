/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description 权限实体类
 */
package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体（菜单 + 按钮统一管理）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission")
public class Permission extends BaseEntity {

    /**
     * 权限名称（显示名）
     */
    private String permissionName;

    /**
     * 权限编码（如 system:role:add）
     */
    private String permissionCode;

    /**
     * 权限类型：MENU-菜单/页面，BUTTON-按钮
     */
    private String type;

    /**
     * 父权限 ID（0 为顶级）
     */
    private Long parentId;

    /**
     * 前端路由路径（MENU 类型使用）
     */
    private String path;

    /**
     * 排序号（升序）
     */
    private Integer sortOrder;

    /**
     * 是否启用（0-停用，1-启用）
     */
    @TableLogic
    private Integer isActive;

    /**
     * 权限描述
     */
    private String description;
}
