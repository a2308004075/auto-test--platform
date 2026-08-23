/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 执行记录响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试执行记录响应
 */
@Data
public class ExecutionResponse {

    private Long id;

    private Long planId;

    private String planName;

    private Long environmentId;

    private String environmentName;

    /**
     * 触发方式：MANUAL / SCHEDULED / CI
     */
    private String triggerType;

    /**
     * 执行状态：PENDING / RUNNING / COMPLETED / FAILED / CANCELLED
     */
    private String status;

    private Integer totalCases;

    private Integer passedCases;

    private Integer failedCases;

    private Integer skippedCases;

    private Integer durationMs;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long triggeredBy;

    private String triggeredByName;

    private LocalDateTime createdAt;

    /**
     * 通过率（百分比，如 91.7）
     */
    private Double passRate;

    /**
     * 执行进度百分比（0~100）
     */
    private Integer progressPercent;
}
