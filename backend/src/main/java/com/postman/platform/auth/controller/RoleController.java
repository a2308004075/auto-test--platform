package com.postman.platform.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.postman.platform.auth.entity.UserRole;
import com.postman.platform.auth.mapper.UserRoleMapper;
import com.postman.platform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final UserRoleMapper userRoleMapper;

    /**
     * 查询全部启用的角色列表
     */
    @GetMapping
    public ApiResponse<List<UserRole>> list() {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(UserRole::getCreatedAt);
        List<UserRole> roles = userRoleMapper.selectList(wrapper);
        return ApiResponse.ok(roles);
    }
}
