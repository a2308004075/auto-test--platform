/**
 * @author HXN
 * @date 2026-08-23
 * @description 环境变量实体类
 */
package com.platform.environment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 环境变量实体（键值对）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("environment_variable")
public class EnvironmentVariable extends BaseEntity {

    private Long environmentId;

    private String varKey;

    private String varValue;

    private String description;

    private Integer sortNo;
}
