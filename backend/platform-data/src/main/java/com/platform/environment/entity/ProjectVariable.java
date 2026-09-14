/**
 * @author HXN
 * @date 2026-08-24
 * @description 项目全局变量实体类
 */
package com.platform.environment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目全局变量实体（键值对，不绑定环境）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_variable")
public class ProjectVariable extends BaseEntity {

    private Long projectId;

    private String varKey;

    private String varValue;

    private String dataType;

    private String description;

    private Integer sortNo;
}
