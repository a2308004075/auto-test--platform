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
     * 状态：0-停用，1-启用
     */
    private Integer status;

    /**
     * 软删除标记（1=已删除，0=未删除）
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
