/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 用户数据访问接口
 */
package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户（仅启用，登录流程使用）
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND is_active = 1")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据用户名查询用户（包含已禁用账号，唯一性校验使用）
     *
     * <p>与 {@link #selectByUsername} 的区别：不带 is_active 条件，
     * 能查出已禁用（逻辑删除）的账号，避免禁用账号同名时唯一性校验被绕过。
     * 登录流程不可使用此方法（禁用账号不应登录）。
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsernameIncludeInactive(@Param("username") String username);

    /**
     * 根据 ID 查询启用的用户
     */
    @Select("SELECT * FROM user WHERE id = #{id} AND is_active = 1")
    User selectActiveById(@Param("id") Long id);
}
