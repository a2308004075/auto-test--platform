/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化用例实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自动化用例实体
 *
 * <p>对应数据库 auto_case 表。步骤树以 JSON 字符串存储。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auto_case")
public class AutoCase extends BaseEntity {

    /**
     * 所属自动化套件 ID
     */
    private Long autoSuiteId;

    /**
     * 所属分组 ID
     */
    private Long groupId;

    /**
     * 自动化用例名称
     */
    private String name;

    /**
     * 自动化用例描述
     */
    private String description;

    /**
     * 前置条件
     */
    private String preconditions;

    /**
     * 自动化用例级 Setup 步骤树（JSON）
     */
    private String setupSteps;

    /**
     * 自动化用例级 Teardown 步骤树（JSON）
     */
    private String teardownSteps;

    /**
     * 自动化用例步骤树（JSON，核心）
     */
    private String steps;

    /**
     * 优先级 P0/P1/P2/P3
     */
    private String priority;

    /**
     * 标签列表（JSON）
     */
    private String tags;

    /**
     * 超时秒数
     */
    private Integer timeout;

    /**
     * 是否启用（0-停用，1-启用）
     */
    private Integer isActive;

    /**
     * 创建人 ID
     */
    private Long createdBy;
}
