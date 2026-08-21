package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    /**
     * 账号（登录名）
     */
    private String username;

    /**
     * bcrypt 哈希密码
     */
    private String passwordHash;

    /**
     * 显示名
     */
    private String displayName;

    /**
     * 角色 ID（关联 user_role.id）
     */
    private Long roleId;

    /**
     * 是否启用（0-停用，1-启用）
     */
    @TableLogic
    private Integer isActive;

    /**
     * 最近登录时间
     */
    private LocalDateTime lastLoginAt;
}
