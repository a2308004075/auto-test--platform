/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description TestResult 响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试结果明细响应
 */
@Data
public class TestResultResponse {

    private Long id;

    private Long executionId;

    private Long autoCaseId;

    /**
     * 自动化用例名称
     */
    private String caseName;

    /**
     * 自动化用例执行结果：PASSED / FAILED / SKIPPED / ERROR
     */
    private String status;

    private String actualResult;

    private String expectedResult;

    private String errorMessage;

    /**
     * 执行日志（JSON）
     */
    private String logs;

    private Integer durationMs;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
