package com.platform.project.dto;

import lombok.Data;

/**
 * 项目仪表板统计数据
 */
@Data
public class DashboardStats {

    private Long apiCount = 0L;
    private Long keywordCount = 0L;
    private Long suiteCount = 0L;
    private Long caseCount = 0L;
    private Long planCount = 0L;
    private Long executionCount = 0L;
    private Long passedCases = 0L;
    private Long failedCases = 0L;
    private Double passRate = 0.0;
}
