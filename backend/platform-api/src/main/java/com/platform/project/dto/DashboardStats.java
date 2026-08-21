package com.platform.project.dto;

import lombok.Data;

/**
 * 项目仪表板统计数据
 */
@Data
public class DashboardStats {

    // ── 基本计数 ──

    private Long apiCount = 0L;
    private Long keywordCount = 0L;
    private Long actionCount = 0L;
    private Long suiteCount = 0L;
    private Long caseCount = 0L;
    private Long planCount = 0L;
    private Long executionCount = 0L;
    private Long passedCases = 0L;
    private Long failedCases = 0L;
    private Double passRate = 0.0;

    // ── 覆盖率 & 完成率 ──

    /** 接口覆盖率（百分比） */
    private Double apiCoverageRate = 0.0;
    /** 已覆盖接口数 */
    private Long coveredApiCount = 0L;
    /** 套件完成率（百分比） */
    private Double suiteCompletionRate = 0.0;
    /** 已完成套件数 */
    private Long completedSuiteCount = 0L;

    // ── 执行 & 回归 ──

    /** 本周执行次数 */
    private Long weeklyExecutionCount = 0L;
    /** 回归通过率 */
    private Double regressionPassRate = 0.0;

    // ── 缺陷相关（暂无数据源，默认 0） ──

    private Double defectDensity = 0.0;
    private Double defectFixTime = 0.0;
    private Double defectEscapeRate = 0.0;

    // ── 质量健康度 ──

    /** 综合评分（0-100） */
    private Integer healthScore = 0;
    /** 通过率得分（权重 35%） */
    private Integer passRateScore = 0;
    /** 覆盖率得分（权重 25%） */
    private Integer coverageScore = 0;
    /** 稳定性得分（权重 25%） */
    private Integer stabilityScore = 0;
    /** 效率得分（权重 15%） */
    private Integer efficiencyScore = 0;
}
