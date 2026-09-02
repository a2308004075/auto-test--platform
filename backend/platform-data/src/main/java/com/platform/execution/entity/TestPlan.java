/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试计划实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试计划实体
 *
 * <p>对应数据库 test_plan 表。auto_suite_ids 以 JSON 数组字符串存储。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_plan")
public class TestPlan extends BaseEntity {

    private Long projectId;

    private String name;

    private String description;

    /**
     * 所属分组 ID（NULL=未分组）
     */
    private Long groupId;

    /**
     * 关联的自动化套件 ID 列表（JSON 数组字符串）
     */
    private String autoSuiteIds;

    /**
     * 关联的手动化用例 ID 列表（JSON 数组字符串）
     */
    private String manualCaseIds;

    /**
     * 默认执行环境 ID
     */
    private Long environmentId;

    /**
     * 定时执行 cron 表达式
     */
    private String scheduleCron;

    /**
     * 触发方式：MANUAL / SCHEDULED / CI
     */
    private String triggerType;

    @TableLogic
    private Integer isActive;

    private Long createdBy;
}
