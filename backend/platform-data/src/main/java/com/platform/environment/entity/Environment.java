/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 环境配置实体类
 */
package com.platform.environment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 环境配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("environment")
public class Environment extends BaseEntity {

    private Long projectId;

    private String name;

    private String description;

    /**
     * 是否为当前激活环境（0-否，1-是）
     */
    @TableField("is_current")
    private Integer isCurrent;
}
