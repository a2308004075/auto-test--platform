/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 用户角色关联实体类
 */
package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_role")
public class UserRole extends BaseEntity {

    /**
     * 角色名称（显示名）
     */
    private String roleName;

    /**
     * 角色编码（如 ADMIN、TESTER）
     */
    private String roleCode;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 排序号（升序）
     */
    private Integer sortOrder;

    /**
     * 是否启用（0-停用，1-启用）
     */
    @TableLogic
    private Integer isActive;
}
