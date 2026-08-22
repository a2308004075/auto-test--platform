/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description RecentExecution
 */
package com.platform.project.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仪表板中最近执行记录摘要
 */
@Data
public class RecentExecution {

    private Long id;
    private Long planId;
    private String planName;
    private String status;
    private Integer totalCases;
    private Integer passedCases;
    private Integer failedCases;
    private Integer durationMs;
    private LocalDateTime createdAt;
}
