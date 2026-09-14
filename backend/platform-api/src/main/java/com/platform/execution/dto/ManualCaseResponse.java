/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动化用例响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 手动化用例响应
 */
@Data
public class ManualCaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long groupId;
    private String title;
    private String preconditions;
    private String operationSteps;
    private String expectedResult;
    private String caseType;
    private String priority;
    private Integer runInTestEnv;
    private Integer runInProdEnv;
    private Integer caseStatus;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
