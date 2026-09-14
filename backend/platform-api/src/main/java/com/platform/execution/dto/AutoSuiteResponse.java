/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 自动化套件响应
 */
@Data
public class AutoSuiteResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private String tags;
    private String priority;
    private Long groupId;
    private Integer enableOnceSetupTeardown;
    private String onceSetupSteps;
    private String onceTeardownSteps;
    private Integer enablePerCaseSetupTeardown;
    private String perCaseSetupSteps;
    private String perCaseTeardownSteps;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 自动化套件下自动化用例数量（附加统计）
     */
    private Long caseCount;
}
