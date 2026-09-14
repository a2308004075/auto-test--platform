/**
 * @author HXN
 * @date 2026-08-20 19:14
 * @description 用户角色数据访问接口
 */
package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.auth.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户角色 Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 根据角色编码查询角色
     */
    @Select("SELECT * FROM user_role WHERE role_code = #{roleCode} AND is_active = 1")
    UserRole selectByCode(@Param("roleCode") String roleCode);
}
