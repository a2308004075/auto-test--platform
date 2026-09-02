/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件内自动化用例级生命周期保存请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 自动化套件内自动化用例级生命周期保存请求
 */
@Data
public class AutoSuiteCaseLifecycleSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 生命周期配置列表（按自动化用例维度批量保存）
     */
    private List<LifecycleItem> items;

    @Data
    public static class LifecycleItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long autoCaseId;

        /**
         * 差异化 Setup 步骤树（JSON），null 表示使用自动化用例自身配置
         */
        private String setupSteps;

        /**
         * 差异化 Teardown 步骤树（JSON），null 表示使用自动化用例自身配置
         */
        private String teardownSteps;
    }
}
