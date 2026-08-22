/**
 * @author HXN
 * @date 2026-08-20 19:14
 * @description 角色管理控制器
 */
package com.platform.auth.controller;

import com.platform.auth.dto.*;
import com.platform.auth.entity.UserRole;
import com.platform.auth.service.RoleService;
import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * 角色管理接口
 *
 * <p>GET /api/v1/roles 为公共接口，供用户管理下拉框使用；
 * 其余接口仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 查询全部启用的角色列表（公共接口）
     */
    @GetMapping
    public ApiResponse<List<UserRole>> list() {
        return ApiResponse.ok(roleService.listActiveRoles());
    }

    /**
     * 分页查询角色列表（ADMIN）
     */
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<RoleResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(roleService.listRolesPage(page, pageSize, keyword));
    }

    /**
     * 查询角色详情（含权限 ID 列表）（ADMIN）
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoleResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(roleService.getRoleDetail(id));
    }

    /**
     * 创建角色（ADMIN）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleService.createRole(request), "角色创建成功");
    }

    /**
     * 更新角色（ADMIN）
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoleResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleService.updateRole(id, request), "角色更新成功");
    }

    /**
     * 删除角色（软删除）（ADMIN）
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success(null, "角色删除成功");
    }

    /**
     * 切换角色状态（ADMIN）
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id,
                                           @RequestBody StatusToggleRequest request) {
        roleService.toggleRoleStatus(id, request.getIsActive());
        return ApiResponse.success(null, "状态变更成功");
    }

    /**
     * 获取角色已分配的权限 ID 列表（ADMIN）
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Long>> rolePermissions(@PathVariable Long id) {
        return ApiResponse.ok(roleService.getRolePermissionIds(id));
    }

    /**
     * 分配权限（ADMIN）
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id,
                                                @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return ApiResponse.success(null, "权限分配成功");
    }

    /**
     * 导出角色列表到 Excel（ADMIN）
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void export(HttpServletResponse response) {
        roleService.exportRoles(response);
    }

    /**
     * 从 Excel 导入角色（ADMIN）
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoleImportResult> importRoles(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(roleService.importRoles(file), "导入完成");
    }
}
