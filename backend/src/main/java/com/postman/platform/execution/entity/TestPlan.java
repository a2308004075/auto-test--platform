package com.postman.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.postman.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试计划实体
 *
 * <p>对应数据库 test_plan 表。suite_ids 以 JSON 数组字符串存储。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_plan")
public class TestPlan extends BaseEntity {

    private Long projectId;

    private String name;

    private String description;

    /**
     * 关联的测试套件 ID 列表（JSON 数组字符串）
     */
    private String suiteIds;

    /**
     * 默认执行环境 ID
     */
    private Long environmentId;

    /**
     * 定时执行 cron 表达式
     */
    private String scheduleCron;

    @TableLogic
    private Boolean isActive;

    private Long createdBy;
}
