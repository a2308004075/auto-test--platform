/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 自动化套件分组实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自动化套件分组实体
 *
 * <p>对应数据库 auto_suite_group 表。通过 parent_id 实现树形层级结构。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auto_suite_group")
public class AutoSuiteGroup extends BaseEntity {

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 父分组 ID（null 表示顶层分组）
     */
    private Long parentId;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组描述
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortNo;
}
