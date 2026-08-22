/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 菜单管理控制器
 */
package com.platform.sys.controller;

import com.platform.auth.entity.User;
import com.platform.auth.service.RoleService;
import com.platform.common.response.ApiResponse;
import com.platform.sys.dto.MenuCreateRequest;
import com.platform.sys.dto.MenuImportResult;
import com.platform.sys.dto.MenuListItem;
import com.platform.sys.dto.MenuTreeNode;
import com.platform.sys.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

/**
 * 菜单管理接口
 * tree() 接口根据用户权限过滤菜单树（侧边栏动态加载和路由生成）
 * 其他 CRUD 接口仅 ADMIN 可访问
 */
@RestController
@RequestMapping("/api/v1/sys/menus")
@RequiredArgsConstructor
public class MenuController {

    /** 系统保留超级管理员账号 */
    private static final String RESERVED_USERNAME = "admin";

    private final MenuService menuService;
    private final RoleService roleService;

    /**
     * 获取菜单树（权限过滤后，供侧边栏动态加载和路由生成）
     * 所有已认证用户均可访问，返回结果按用户权限过滤
     */
    @GetMapping("/tree")
    public ApiResponse<List<MenuTreeNode>> tree(@AuthenticationPrincipal User user) {
        if (user == null) {
            // 未登录时返回空菜单树
            return ApiResponse.ok(Collections.emptyList());
        }
        // admin 账号保护：强制返回全部菜单，不受角色管理配置影响
        List<String> permissionCodes;
        if (RESERVED_USERNAME.equalsIgnoreCase(user.getUsername())) {
            permissionCodes = Collections.singletonList("*");
        } else {
            permissionCodes = roleService.getPermissionCodesByRoleId(user.getRoleId());
        }
        return ApiResponse.ok(menuService.tree(permissionCodes));
    }

    /**
     * 获取所有菜单列表（扁平，含停用，供管理页面使用）
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<MenuListItem>> list() {
        return ApiResponse.ok(menuService.listAll());
    }

    /**
     * 获取单个菜单
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<MenuListItem> get(@PathVariable Long id) {
        return ApiResponse.ok(menuService.get(id));
    }

    /**
     * 新增菜单
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<MenuListItem> add(@Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.ok(menuService.add(request));
    }

    /**
     * 更新菜单
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<MenuListItem> update(@PathVariable Long id,
                                            @Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.ok(menuService.update(id, request));
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.ok(null);
    }

    /**
     * 切换菜单启用/停用状态
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id) {
        menuService.toggleStatus(id);
        return ApiResponse.ok(null);
    }

    /**
     * 导出菜单列表到 Excel（仅 ADMIN）
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void export(HttpServletResponse response) {
        menuService.exportMenus(response);
    }

    /**
     * 从 Excel 导入菜单（仅 ADMIN）
     */
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<MenuImportResult> importMenus(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(menuService.importMenus(file));
    }
}
