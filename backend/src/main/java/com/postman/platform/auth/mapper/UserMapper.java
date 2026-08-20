package com.postman.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.postman.platform.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND is_active = 1")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据 ID 查询启用的用户
     */
    @Select("SELECT * FROM user WHERE id = #{id} AND is_active = 1")
    User selectActiveById(@Param("id") Long id);
}
