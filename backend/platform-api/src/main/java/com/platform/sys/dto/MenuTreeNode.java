/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description MenuTreeNode
 */
package com.platform.sys.dto;

import lombok.Data;

import java.util.List;

/**
 * 菜单树节点响应
 */
@Data
public class MenuTreeNode {

    private Long id;
    private Long parentId;
    private String name;
    private Integer menuType;
    private String icon;
    private String routePath;
    private Integer sortNo;
    private Integer isActive;

    /**
     * 子菜单列表
     */
    private List<MenuTreeNode> children;
}
