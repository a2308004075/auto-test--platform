/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description MenuListItem
 */
package com.platform.sys.dto;

import lombok.Data;

/**
 * 菜单列表项响应（扁平结构，用于表格展示）
 */
@Data
public class MenuListItem {

    private Long id;
    private Long parentId;
    private String name;
    private Integer menuType;
    private String icon;
    private String routePath;
    private String component;
    private Integer sortNo;
    private Integer isActive;
    private String createdAt;
    private String updatedAt;
}
