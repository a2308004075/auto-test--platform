package com.postman.platform.execution.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试计划响应
 */
@Data
public class PlanResponse {

    private String id;

    private String projectId;

    private String name;

    private String description;

    /**
     * 关联的测试套件 ID 列表
     */
    private List<String> suiteIds;

    private String environmentId;

    private String environmentName;

    private String scheduleCron;

    private Boolean isActive;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
