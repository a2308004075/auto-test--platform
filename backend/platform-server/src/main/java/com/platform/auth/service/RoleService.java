/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 角色管理服务
 */
package com.platform.auth.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.auth.dto.*;
import com.platform.auth.entity.Permission;
import com.platform.auth.entity.RolePermission;
import com.platform.auth.entity.User;
import com.platform.auth.entity.UserRole;
import com.platform.auth.mapper.PermissionMapper;
import com.platform.auth.mapper.RolePermissionMapper;
import com.platform.auth.mapper.UserMapper;
import com.platform.auth.mapper.UserRoleMapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.exception.NotFoundException;
import com.platform.common.response.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理服务
 *
 * <p>提供角色 CRUD、权限分配、Excel 导入导出等功能。
 * ADMIN 角色为系统内置，不可删除/禁用/修改权限，且隐式拥有全部权限。
 */
@Slf4j
@Service
public class RoleService {

    private static final String BUILTIN_ROLE_CODE = "ADMIN";

    private final UserRoleMapper userRoleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserMapper userMapper;

    public RoleService(UserRoleMapper userRoleMapper,
                       PermissionMapper permissionMapper,
                       RolePermissionMapper rolePermissionMapper,
                       UserMapper userMapper) {
        this.userRoleMapper = userRoleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userMapper = userMapper;
    }

    // ===== 公共接口 =====

    /**
     * 查询全部启用的角色列表（供下拉框使用）
     */
    public List<UserRole> listActiveRoles() {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(UserRole::getSortOrder)
                .orderByAsc(UserRole::getCreatedAt);
        return userRoleMapper.selectList(wrapper);
    }

    // ===== 管理接口 =====

    /**
     * 分页查询角色列表
     */
    public PageResponse<RoleResponse> listRolesPage(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(UserRole::getRoleName, keyword)
                    .or().like(UserRole::getRoleCode, keyword));
        }
        wrapper.orderByAsc(UserRole::getSortOrder)
                .orderByAsc(UserRole::getCreatedAt);

        Page<UserRole> pageParam = new Page<>(page, pageSize);
        Page<UserRole> result = userRoleMapper.selectPage(pageParam, wrapper);
        List<RoleResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 查询角色详情（含权限 ID 列表）
     */
    public RoleResponse getRoleDetail(Long id) {
        UserRole role = userRoleMapper.selectById(id);
        if (role == null) {
            throw new NotFoundException("角色", id);
        }
        RoleResponse response = toResponse(role);
        response.setPermissionIds(rolePermissionMapper.selectPermissionIdsByRoleId(id));
        return response;
    }

    /**
     * 创建角色
     */
    @Transactional(rollbackFor = Exception.class)
    public RoleResponse createRole(RoleCreateRequest request) {
        // 校验角色编码唯一性
        checkRoleCodeDuplicate(request.getRoleCode(), null);

        UserRole role = new UserRole();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode().toUpperCase());
        role.setDescription(request.getDescription());
        role.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        role.setIsActive(1);
        userRoleMapper.insert(role);

        // 分配权限
        if (CollUtil.isNotEmpty(request.getPermissionIds())) {
            bindPermissions(role.getId(), request.getPermissionIds());
        }

        log.info("创建角色成功: roleCode={}", role.getRoleCode());
        return getRoleDetail(role.getId());
    }

    /**
     * 更新角色
     */
    @Transactional(rollbackFor = Exception.class)
    public RoleResponse updateRole(Long id, RoleCreateRequest request) {
        UserRole role = userRoleMapper.selectById(id);
        if (role == null) {
            throw new NotFoundException("角色", id);
        }
        checkBuiltinRole(role);

        // 校验角色编码唯一性
        checkRoleCodeDuplicate(request.getRoleCode(), id);

        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode().toUpperCase());
        role.setDescription(request.getDescription());
        if (request.getSortOrder() != null) {
            role.setSortOrder(request.getSortOrder());
        }
        userRoleMapper.updateById(role);

        // 重建权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, id));
        if (CollUtil.isNotEmpty(request.getPermissionIds())) {
            bindPermissions(id, request.getPermissionIds());
        }

        log.info("更新角色成功: id={}, roleCode={}", id, role.getRoleCode());
        return getRoleDetail(id);
    }

    /**
     * 删除角色（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        UserRole role = userRoleMapper.selectById(id);
        if (role == null) {
            throw new NotFoundException("角色", id);
        }
        checkBuiltinRole(role);

        // 检查是否有用户引用此角色
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRoleId, id));
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_HAS_USERS,
                    "该角色下还有 " + userCount + " 个用户，无法删除");
        }

        // 删除权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, id));

        // 软删除角色
        userRoleMapper.deleteById(id);
        log.info("删除角色成功: id={}, roleCode={}", id, role.getRoleCode());
    }

    /**
     * 切换角色状态（启用/停用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleRoleStatus(Long id, Integer isActive) {
        UserRole role = userRoleMapper.selectById(id);
        if (role == null) {
            throw new NotFoundException("角色", id);
        }
        checkBuiltinRole(role);
        role.setIsActive(isActive);
        userRoleMapper.updateById(role);
        log.info("角色状态变更: id={}, isActive={}", id, isActive);
    }

    // ===== 权限管理 =====

    /**
     * 获取全量权限树
     */
    public List<PermissionTreeNode> getPermissionTree() {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Permission::getSortOrder)
                .orderByAsc(Permission::getId);
        List<Permission> allPermissions = permissionMapper.selectList(wrapper);
        return buildTree(allPermissions);
    }

    /**
     * 获取角色已分配的权限 ID 列表
     */
    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    /**
     * 分配权限（先删后插）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        UserRole role = userRoleMapper.selectById(roleId);
        if (role == null) {
            throw new NotFoundException("角色", roleId);
        }
        checkBuiltinRole(role);

        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));
        if (CollUtil.isNotEmpty(permissionIds)) {
            bindPermissions(roleId, permissionIds);
        }
        log.info("角色权限分配成功: roleId={}, permissionCount={}", roleId, permissionIds.size());
    }

    /**
     * 获取角色的权限编码列表（供 AuthService 调用）
     * ADMIN 角色返回 ["*"] 表示拥有全部权限
     */
    public List<String> getPermissionCodesByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        UserRole role = userRoleMapper.selectById(roleId);
        if (role == null) {
            return Collections.emptyList();
        }
        if (BUILTIN_ROLE_CODE.equalsIgnoreCase(role.getRoleCode())) {
            return Collections.singletonList("*");
        }
        return rolePermissionMapper.selectPermissionCodesByRoleId(roleId);
    }

    // ===== Excel 导入导出 =====

    /**
     * 导出角色列表到 Excel
     */
    public void exportRoles(HttpServletResponse response) {
        List<UserRole> roles = listActiveRoles();

        // 收集每个角色的权限编码
        List<List<Object>> rows = new ArrayList<>();
        for (UserRole role : roles) {
            List<String> permissionCodes;
            if (BUILTIN_ROLE_CODE.equalsIgnoreCase(role.getRoleCode())) {
                permissionCodes = Collections.singletonList("*");
            } else {
                permissionCodes = rolePermissionMapper.selectPermissionCodesByRoleId(role.getId());
            }
            List<Object> row = new ArrayList<>();
            row.add(role.getRoleName());
            row.add(role.getRoleCode());
            row.add(role.getDescription() != null ? role.getDescription() : "");
            row.add(role.getSortOrder() != null ? role.getSortOrder() : 0);
            row.add(Integer.valueOf(1).equals(role.getIsActive()) ? "启用" : "停用");
            row.add(String.join(",", permissionCodes));
            rows.add(row);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode("角色列表.xlsx", "UTF-8"));
            OutputStream out = response.getOutputStream();
            ExcelWriter writer = cn.hutool.poi.excel.ExcelUtil.getWriter(true);
            writer.writeCellValue(0, 0, "角色名称");
            writer.writeCellValue(1, 0, "角色编码");
            writer.writeCellValue(2, 0, "描述");
            writer.writeCellValue(3, 0, "排序号");
            writer.writeCellValue(4, 0, "状态");
            writer.writeCellValue(5, 0, "权限编码");
            for (int i = 0; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                for (int j = 0; j < row.size(); j++) {
                    writer.writeCellValue(j, i + 1, row.get(j));
                }
            }
            writer.flush(out, true);
            writer.close();
        } catch (IOException e) {
            log.error("导出角色 Excel 失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 从 Excel 导入角色
     */
    @Transactional(rollbackFor = Exception.class)
    public RoleImportResult importRoles(MultipartFile file) {
        RoleImportResult result = new RoleImportResult();
        if (file == null || file.isEmpty()) {
            result.getErrors().add("文件为空");
            return result;
        }

        try {
            ExcelReader reader = cn.hutool.poi.excel.ExcelUtil.getReader(file.getInputStream());
            List<List<Object>> rows = reader.read();
            reader.close();

            // 跳过表头行
            for (int i = 1; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                if (row.isEmpty()) {
                    continue;
                }
                try {
                    String roleName = row.size() > 0 ? String.valueOf(row.get(0)).trim() : "";
                    String roleCode = row.size() > 1 ? String.valueOf(row.get(1)).trim() : "";
                    String description = row.size() > 2 ? String.valueOf(row.get(2)).trim() : "";
                    int sortOrder = row.size() > 3 ? parseIntSafe(row.get(3)) : 0;

                    if (roleName.isEmpty() || roleCode.isEmpty()) {
                        result.getErrors().add("第 " + (i + 1) + " 行: 角色名称和编码不能为空");
                        result.setFailCount(result.getFailCount() + 1);
                        continue;
                    }

                    // 跳过 ADMIN 内置角色
                    if (BUILTIN_ROLE_CODE.equalsIgnoreCase(roleCode)) {
                        result.getErrors().add("第 " + (i + 1) + " 行: ADMIN 为系统内置角色，已跳过");
                        result.setFailCount(result.getFailCount() + 1);
                        continue;
                    }

                    // 按 roleCode 查找是否已存在
                    LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(UserRole::getRoleCode, roleCode.toUpperCase());
                    UserRole existing = userRoleMapper.selectOne(wrapper);

                    if (existing != null) {
                        // 更新
                        existing.setRoleName(roleName);
                        existing.setDescription(description);
                        existing.setSortOrder(sortOrder);
                        userRoleMapper.updateById(existing);
                    } else {
                        // 新建
                        UserRole role = new UserRole();
                        role.setRoleName(roleName);
                        role.setRoleCode(roleCode.toUpperCase());
                        role.setDescription(description);
                        role.setSortOrder(sortOrder);
                        role.setIsActive(1);
                        userRoleMapper.insert(role);
                    }
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception e) {
                    result.getErrors().add("第 " + (i + 1) + " 行: " + e.getMessage());
                    result.setFailCount(result.getFailCount() + 1);
                }
            }
        } catch (IOException e) {
            log.error("导入角色 Excel 失败", e);
            throw new BusinessException(ErrorCode.EXCEL_IMPORT_FAILED, "文件读取失败: " + e.getMessage());
        }

        log.info("角色导入完成: 成功={}, 失败={}", result.getSuccessCount(), result.getFailCount());
        return result;
    }

    // ===== 私有方法 =====

    private void checkRoleCodeDuplicate(String roleCode, Long excludeId) {
        if (roleCode == null || roleCode.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getRoleCode, roleCode.toUpperCase());
        if (excludeId != null) {
            wrapper.ne(UserRole::getId, excludeId);
        }
        Long count = userRoleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.ROLE_CODE_DUPLICATE, "角色编码已存在: " + roleCode);
        }
    }

    private void checkBuiltinRole(UserRole role) {
        if (BUILTIN_ROLE_CODE.equalsIgnoreCase(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.ROLE_IS_BUILTIN, "系统内置角色不可修改或删除");
        }
    }

    private void bindPermissions(Long roleId, List<Long> permissionIds) {
        for (Long permissionId : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rolePermissionMapper.insert(rp);
        }
    }

    private List<PermissionTreeNode> buildTree(List<Permission> permissions) {
        Map<Long, PermissionTreeNode> nodeMap = new LinkedHashMap<>();
        for (Permission p : permissions) {
            PermissionTreeNode node = new PermissionTreeNode();
            node.setId(p.getId());
            node.setPermissionName(p.getPermissionName());
            node.setPermissionCode(p.getPermissionCode());
            node.setType(p.getType());
            node.setParentId(p.getParentId());
            node.setPath(p.getPath());
            node.setSortOrder(p.getSortOrder());
            node.setDescription(p.getDescription());
            nodeMap.put(p.getId(), node);
        }
        List<PermissionTreeNode> roots = new ArrayList<>();
        for (PermissionTreeNode node : nodeMap.values()) {
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
            } else {
                PermissionTreeNode parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    private RoleResponse toResponse(UserRole role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleName(role.getRoleName());
        response.setRoleCode(role.getRoleCode());
        response.setDescription(role.getDescription());
        response.setSortOrder(role.getSortOrder());
        response.setIsActive(role.getIsActive());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        return response;
    }

    private int parseIntSafe(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
