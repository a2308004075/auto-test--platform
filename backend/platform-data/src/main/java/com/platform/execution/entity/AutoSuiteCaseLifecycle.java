/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件内自动化用例级生命周期实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自动化套件内自动化用例级生命周期实体
 *
 * <p>对应数据库 auto_suite_case_lifecycle 表。
 * 用于存储自动化套件内某条自动化用例差异化的 Setup/Teardown 步骤树，
 * 优先级高于自动化用例自身的 setup_steps / teardown_steps。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auto_suite_case_lifecycle")
public class AutoSuiteCaseLifecycle extends BaseEntity {

    /**
     * 所属自动化套件 ID
     */
    private Long autoSuiteId;

    /**
     * 所属自动化用例 ID
     */
    private Long autoCaseId;

    /**
     * 自动化套件内该自动化用例差异化 Setup 步骤树（JSON）
     */
    private String setupSteps;

    /**
     * 自动化套件内该自动化用例差异化 Teardown 步骤树（JSON）
     */
    private String teardownSteps;
}
