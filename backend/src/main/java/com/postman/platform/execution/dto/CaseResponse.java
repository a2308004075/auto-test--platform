package com.postman.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试用例响应
 */
@Data
public class CaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String suiteId;
    private String name;
    private String description;
    private String preconditions;
    private String setupSteps;
    private String teardownSteps;
    private String steps;
    private String priority;
    private Integer timeout;
    private Boolean isActive;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
