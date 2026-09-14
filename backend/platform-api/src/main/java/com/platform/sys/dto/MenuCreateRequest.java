/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description 菜单创建请求 DTO
 */
package com.platform.sys.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 菜单创建/更新请求
 */
@Data
public class MenuCreateRequest {

    /**
     * 父级菜单 ID（0 为顶级）
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    /**
     * 菜单类型：1=目录 2=菜单 3=按钮
     */
    @NotNull(message = "菜单类型不能为空")
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
     * 前端组件路径（用于动态路由）
     */
    private String component;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 关联权限编码（对应 permission.permission_code）
     */
    private String permissionCode;
}
