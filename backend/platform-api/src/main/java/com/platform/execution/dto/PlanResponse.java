package com.platform.execution.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试计划响应
 */
@Data
public class PlanResponse {

    private Long id;

    private Long projectId;

    private String name;

    private String description;

    /**
     * 关联的测试套件 ID 列表
     */
    private List<Long> suiteIds;

    private Long environmentId;

    private String environmentName;

    private String scheduleCron;

    private Boolean isActive;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
