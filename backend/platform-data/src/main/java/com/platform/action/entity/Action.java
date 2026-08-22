/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 关键字实体类
 */
package com.platform.action.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Action 关键字实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("action")
public class Action extends BaseEntity {

    private Long projectId;

    private String name;

    private String description;

    /**
     * 节点配置（JSON 数组）
     */
    private String nodes;

    /**
     * 输入参数定义（JSON）
     */
    private String inputParams;

    /**
     * 输出参数定义（JSON）
     */
    private String outputParams;

    @TableLogic
    private Integer isActive;

    private Long createdBy;

    private Long updatedBy;
}
