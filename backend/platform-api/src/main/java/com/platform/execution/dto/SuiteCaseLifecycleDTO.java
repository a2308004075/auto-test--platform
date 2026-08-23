/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 套件内用例级生命周期 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 套件内用例级生命周期 DTO
 */
@Data
public class SuiteCaseLifecycleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long suiteId;
    private Long caseId;

    /**
     * 用例名称（附加信息，便于前端展示）
     */
    private String caseName;

    /**
     * 套件内该用例差异化 Setup 步骤树（JSON）
     */
    private String setupSteps;

    /**
     * 套件内该用例差异化 Teardown 步骤树（JSON）
     */
    private String teardownSteps;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
