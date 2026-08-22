/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试用例实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试用例实体
 *
 * <p>对应数据库 test_case 表。步骤树以 JSON 字符串存储。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_case")
public class TestCase extends BaseEntity {

    /**
     * 所属测试套件 ID
     */
    private Long suiteId;

    /**
     * 用例名称
     */
    private String name;

    /**
     * 用例描述
     */
    private String description;

    /**
     * 前置条件
     */
    private String preconditions;

    /**
     * 用例级 Setup 步骤树（JSON）
     */
    private String setupSteps;

    /**
     * 用例级 Teardown 步骤树（JSON）
     */
    private String teardownSteps;

    /**
     * 用例步骤树（JSON，核心）
     */
    private String steps;

    /**
     * 优先级 P0/P1/P2/P3
     */
    private String priority;

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
