/**
 * @author HXN
 * @date 2026-08-23
 * @description 测试计划分组实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试计划分组实体
 *
 * <p>对应数据库 plan_group 表。支持通过 parent_id 实现树形嵌套。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plan_group")
public class PlanGroup extends BaseEntity {

    private Long projectId;

    private String name;

    private String description;

    /**
     * 父分组 ID（NULL=顶级分组）
     */
    private Long parentId;

    private Integer sortOrder;
}
