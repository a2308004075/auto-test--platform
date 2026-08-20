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

    private String host;

    private Integer port;

    private String databaseName;

    private String username;

    private String password;

    /**
     * 额外配置（JSON 格式）
     */
    private String configJson;

    /**
     * 是否为当前激活环境（对应 DB 的 is_current 字段）
     */
    @TableField("is_current")
    private Boolean isCurrent;
}
