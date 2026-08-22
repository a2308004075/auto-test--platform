/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 权限管理控制器
 */
package com.platform.auth.controller;

import com.platform.auth.dto.PermissionSyncResult;
import com.platform.auth.dto.PermissionTreeNode;
import com.platform.auth.service.RoleService;
import com.platform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理接口（ADMIN）
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final RoleService roleService;

    /**
     * 获取全量权限树
     */
    @GetMapping("/tree")
    public ApiResponse<List<PermissionTreeNode>> tree() {
        return ApiResponse.ok(roleService.getPermissionTree());
    }

    /**
     * 同步权限：从 sys_menu 表同步页面和按钮到 permission 表
     *
     * <p>扫描 sys_menu 中所有设置了 permission_code 的菜单条目，
     * 在 permission 表中创建缺失的权限或更新已有权限的名称/路径/排序号。
     */
    @PostMapping("/sync")
    public ApiResponse<PermissionSyncResult> sync() {
        return ApiResponse.success(roleService.syncPermissions(), "权限同步完成");
    }
}
