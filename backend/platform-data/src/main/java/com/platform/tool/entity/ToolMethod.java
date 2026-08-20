package com.platform.tool.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具方法实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_method")
public class ToolMethod extends BaseEntity {

    private Long projectId;

    private String name;

    /**
     * 分类：BUILTIN（内置）/ CUSTOM（自定义）
     */
    private String category;

    private String description;

    /**
     * 参数定义（JSON 数组）
     */
    private String paramDefinitions;

    private String returnType;

    /**
     * Groovy 代码
     */
    private String code;

    @TableLogic
    private Boolean isActive;

    private String testInput;

    private String testResult;

    private Long createdBy;

    private Long updatedBy;
}
