/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 角色权限数据访问接口
 */
package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.auth.dto.PermissionAssignmentDTO;
import com.platform.auth.dto.PermissionBriefDTO;
import com.platform.auth.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联 Mapper
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 查询角色已分配的权限 ID 列表
     */
    @Select("SELECT permission_id FROM role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询角色已分配的权限编码列表（JOIN permission 表）
     */
    @Select("SELECT p.permission_code FROM role_permission rp " +
            "INNER JOIN permission p ON rp.permission_id = p.id " +
            "WHERE rp.role_id = #{roleId} AND p.is_active = 1")
    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询角色已分配的权限列表（含按角色 control_mode）
     */
    @Select("SELECT rp.permission_id AS permissionId, rp.control_mode AS controlMode " +
            "FROM role_permission rp " +
            "INNER JOIN permission p ON rp.permission_id = p.id " +
            "WHERE rp.role_id = #{roleId} AND p.is_active = 1")
    List<PermissionAssignmentDTO> selectPermissionAssignmentsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询角色权限详情列表（code + type + 按角色 control_mode）
     * 用于登录响应和 /me 接口的 permissionDetails 构建
     */
    @Select("SELECT p.permission_code AS `code`, p.type, rp.control_mode AS controlMode " +
            "FROM role_permission rp " +
            "INNER JOIN permission p ON rp.permission_id = p.id " +
            "WHERE rp.role_id = #{roleId} AND p.is_active = 1")
    List<PermissionBriefDTO> selectPermissionBriefsByRoleId(@Param("roleId") Long roleId);
}
