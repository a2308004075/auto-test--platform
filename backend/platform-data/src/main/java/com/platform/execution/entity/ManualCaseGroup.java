/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动化用例分组实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 手动化用例分组实体
 *
 * <p>对应数据库 manual_case_group 表。支持树形结构（parentId），
 * 区分系统分组和用户自定义分组。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("manual_case_group")
public class ManualCaseGroup extends BaseEntity {

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 父分组 ID（null=根分组）
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
