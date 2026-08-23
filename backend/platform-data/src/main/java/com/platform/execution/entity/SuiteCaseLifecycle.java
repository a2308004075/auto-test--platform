/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 套件内用例级生命周期实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 套件内用例级生命周期实体
 *
 * <p>对应数据库 suite_case_lifecycle 表。
 * 用于存储套件内某条用例差异化的 Setup/Teardown 步骤树，
 * 优先级高于用例自身的 setup_steps / teardown_steps。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("suite_case_lifecycle")
public class SuiteCaseLifecycle extends BaseEntity {

    /**
     * 所属测试套件 ID
     */
    private Long suiteId;

    /**
     * 所属测试用例 ID
     */
    private Long caseId;

    /**
     * 套件内该用例差异化 Setup 步骤树（JSON）
     */
    private String setupSteps;

    /**
     * 套件内该用例差异化 Teardown 步骤树（JSON）
     */
    private String teardownSteps;
}
