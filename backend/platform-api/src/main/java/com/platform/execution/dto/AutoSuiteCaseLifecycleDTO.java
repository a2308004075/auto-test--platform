/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件内自动化用例级生命周期 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 自动化套件内自动化用例级生命周期 DTO
 */
@Data
public class AutoSuiteCaseLifecycleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long autoSuiteId;
    private Long autoCaseId;

    /**
     * 自动化用例名称（附加信息，便于前端展示）
     */
    private String caseName;

    /**
     * 自动化套件内该自动化用例差异化 Setup 步骤树（JSON）
     */
    private String setupSteps;

    /**
     * 自动化套件内该自动化用例差异化 Teardown 步骤树（JSON）
     */
    private String teardownSteps;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
