/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 角色权限数据访问接口
 */
package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
