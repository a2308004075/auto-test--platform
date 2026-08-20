package com.postman.platform.execution.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试结果明细响应
 */
@Data
public class TestResultResponse {

    private Long id;

    private Long executionId;

    private Long caseId;

    private String caseName;

    /**
     * 用例执行结果：PASSED / FAILED / SKIPPED / ERROR
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
