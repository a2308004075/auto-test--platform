/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试套件实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试套件实体
 *
 * <p>对应数据库 test_suite 表。步骤树（Setup/Teardown/Steps）以 JSON 字符串存储，
 * 由 Service 层通过 ObjectMapper 序列化/反序列化。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_suite")
public class TestSuite extends BaseEntity {

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 套件名称
     */
    private String name;

    /**
     * 套件描述
     */
    private String description;

    /**
     * 标签列表（JSON 数组字符串）
     */
    private String tags;

    /**
     * 优先级 P0/P1/P2/P3
     */
    private String priority;

    /**
     * 套件级·整体 Setup 步骤树（JSON）
     */
    private String onceSetupSteps;

    /**
     * 套件级·整体 Teardown 步骤树（JSON）
     */
    private String onceTeardownSteps;

    /**
     * 是否启用套件级·整体生命周期（0-否，1-是）
     */
    private Integer enableOnceSetupTeardown;

    /**
     * 套件级·每条用例 Setup 步骤树（JSON）
     */
    private String perCaseSetupSteps;

    /**
     * 套件级·每条用例 Teardown 步骤树（JSON）
     */
    private String perCaseTeardownSteps;

    /**
     * 是否启用套件级·每条用例生命周期（0-否，1-是）
     */
    private Integer enablePerCaseSetupTeardown;

    /**
     * 创建人 ID
     */
    private Long createdBy;
}
