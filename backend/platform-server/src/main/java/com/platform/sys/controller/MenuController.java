/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 菜单管理控制器
 */
package com.platform.sys.controller;

import com.platform.common.response.ApiResponse;
import com.platform.sys.dto.MenuCreateRequest;
import com.platform.sys.dto.MenuListItem;
import com.platform.sys.dto.MenuTreeNode;
import com.platform.sys.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 菜单管理接口（仅 ADMIN）
 */
@RestController
@RequestMapping("/api/v1/sys/menus")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MenuController {

    private final MenuService menuService;

    /**
     * 获取菜单树（仅启用状态，供侧边栏动态加载）
     */
    @GetMapping("/tree")
    public ApiResponse<List<MenuTreeNode>> tree() {
        return ApiResponse.ok(menuService.tree());
    }

    /**
     * 获取所有菜单列表（扁平，含停用，供管理页面使用）
     */
    @GetMapping
    public ApiResponse<List<MenuListItem>> list() {
        return ApiResponse.ok(menuService.listAll());
    }

    /**
     * 获取单个菜单
     */
    @GetMapping("/{id}")
    public ApiResponse<MenuListItem> get(@PathVariable Long id) {
        return ApiResponse.ok(menuService.get(id));
    }

    /**
     * 新增菜单
     */
    @PostMapping
    public ApiResponse<MenuListItem> add(@Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.ok(menuService.add(request));
    }

    /**
     * 更新菜单
     */
    @PostMapping("/{id}")
    public ApiResponse<MenuListItem> update(@PathVariable Long id,
                                            @Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.ok(menuService.update(id, request));
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.ok(null);
    }

    /**
     * 切换菜单启用/停用状态
     */
    @PostMapping("/{id}/toggle")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id) {
        menuService.toggleStatus(id);
        return ApiResponse.ok(null);
    }
}
