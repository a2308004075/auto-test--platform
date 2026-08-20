package com.postman.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试套件响应
 */
@Data
public class SuiteResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private String tags;
    private String priority;
    private Boolean enableOnceSetupTeardown;
    private String onceSetupSteps;
    private String onceTeardownSteps;
    private Boolean enablePerCaseSetupTeardown;
    private String perCaseSetupSteps;
    private String perCaseTeardownSteps;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 套件下用例数量（附加统计）
     */
    private Long caseCount;
}
