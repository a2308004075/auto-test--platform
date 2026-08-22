/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description 权限树节点 DTO
 */
package com.platform.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限树节点（前端 el-tree 使用）
 */
@Data
public class PermissionTreeNode {

    private Long id;
    private String permissionName;
    private String permissionCode;
    private String type;
    private Long parentId;
    private String path;
    private Integer sortOrder;
    private String description;

    /**
     * 子权限列表
     */
    private List<PermissionTreeNode> children = new ArrayList<>();
}
