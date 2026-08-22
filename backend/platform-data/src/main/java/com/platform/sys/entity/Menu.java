/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 菜单实体类
 */
package com.platform.sys.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统菜单实体
 *
 * <p>菜单类型：1=目录 2=菜单 3=按钮
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class Menu extends BaseEntity {

    /**
     * 父级菜单 ID（0 为顶级）
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单类型：1=目录 2=菜单 3=按钮
     */
    private Integer menuType;

    /**
     * 图标名称
     */
    private String icon;

    /**
     * 路由路径
     */
    private String routePath;

    /**
     * 排序号（升序）
     */
    private Integer sortNo;

    /**
     * 是否启用（1=启用 0=停用）
     */
    @TableLogic
    private Integer isActive;
}
