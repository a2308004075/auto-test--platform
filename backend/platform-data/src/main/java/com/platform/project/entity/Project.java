package com.platform.project.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends BaseEntity {

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 项目源码路径
     */
    private String sourcePath;

    /**
     * 是否启用（启用/停用切换，非软删除）
     */
    private Boolean isActive;

    /**
     * 软删除标记（true=已删除）
     */
    @TableLogic
    private Boolean deleted;
}
