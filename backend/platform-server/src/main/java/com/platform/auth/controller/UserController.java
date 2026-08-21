package com.platform.auth.controller;

import com.platform.auth.dto.*;
import com.platform.auth.service.UserService;
import com.platform.common.response.ApiResponse;
import com.platform.common.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户管理控制器（仅 ADMIN 可操作）
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户列表（分页）
     *
     * @param keyword     通用关键词（同时模糊搜索账号和用户名，向后兼容）
     * @param account     独立搜索账号（username）
     * @param displayName 独立搜索用户名（display_name）
     * @param roleId      角色 ID 筛选
     */
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) Long roleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<UserResponse> response = userService.listUsers(keyword, account, displayName, roleId, page, pageSize);
        return ApiResponse.success(response);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.success(response);
    }

    /**
     * 更新用户
     */
    @PostMapping("/{userId}")
    public ApiResponse<UserResponse> update(@PathVariable Long userId,
                                            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 删除用户
     */
    @PostMapping("/{userId}/delete")
    public ApiResponse<Void> delete(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.success(null, "删除成功");
    }

    /**
     * 启用/禁用用户
     */
    @PostMapping("/{userId}/status")
    public ApiResponse<UserResponse> toggleStatus(@PathVariable Long userId,
                                                  @Valid @RequestBody StatusToggleRequest request) {
        UserResponse response = userService.toggleStatus(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 重置密码
     */
    @PostMapping("/{userId}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long userId,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(userId, request);
        return ApiResponse.success(null, "密码重置成功");
    }
}
