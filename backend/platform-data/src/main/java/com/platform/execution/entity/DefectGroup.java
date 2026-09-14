/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷分组实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 缺陷分组实体
 *
 * <p>对应数据库 defect_group 表。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("defect_group")
public class DefectGroup extends BaseEntity {

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 父分组 ID
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
     * 是否系统默认分组（0-否，1-是）
     */
    private Integer isSystem;

    /**
     * 创建人 ID
     */
    private Long createdBy;
}
