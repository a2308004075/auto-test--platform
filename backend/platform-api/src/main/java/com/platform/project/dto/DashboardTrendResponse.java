/**
 * @author HXN
 * @date 2026-08-21 16:19
 * @description DashboardTrend 响应 DTO
 */
package com.platform.project.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 仪表板趋势数据响应
 */
@Data
public class DashboardTrendResponse {

    /** 通过率趋势点 */
    private List<TrendPoint> passRateTrend = new ArrayList<>();

    /** 每日执行频次 */
    private List<ExecutionFreqItem> executionFrequency = new ArrayList<>();

    /** 模块覆盖率 */
    private List<ModuleCoverage> moduleCoverage = new ArrayList<>();

    /** 质量风险 Top 5 */
    private List<QualityRisk> qualityRiskTop5 = new ArrayList<>();

    /** 缺陷趋势（近 4 周） */
    private List<DefectTrendItem> defectTrend = new ArrayList<>();

    /** 持续失败用例数 */
    private Integer continuousFailCount = 0;

    /** 最近执行时间 */
    private LocalDateTime lastExecutionTime;

    /** 数据更新时间 */
    private LocalDateTime dataUpdateTime;

    // ────────────── 嵌套 DTO ──────────────

    /**
     * 趋势数据点（日期 + 值）
     */
    @Data
    public static class TrendPoint {
        private String date;
        private Double value;

        public TrendPoint() {}

        public TrendPoint(String date, Double value) {
            this.date = date;
            this.value = value;
        }
    }

    /**
     * 每日执行频次（通过/失败堆叠）
     */
    @Data
    public static class ExecutionFreqItem {
        private String day;
        private Integer passed;
        private Integer failed;

        public ExecutionFreqItem() {}

        public ExecutionFreqItem(String day, Integer passed, Integer failed) {
            this.day = day;
            this.passed = passed;
            this.failed = failed;
        }
    }

    /**
     * 模块覆盖率
     */
    @Data
    public static class ModuleCoverage {
        private String moduleName;
        private Long count;
        private Double percentage;

        public ModuleCoverage() {}

        public ModuleCoverage(String moduleName, Long count, Double percentage) {
            this.moduleName = moduleName;
            this.count = count;
            this.percentage = percentage;
        }
    }

    /**
     * 质量风险项
     */
    @Data
    public static class QualityRisk {
        private Integer rank;
        private String caseName;
        private String suiteName;
        private Integer failCount;
        private Double failRate;

        public QualityRisk() {}

        public QualityRisk(Integer rank, String caseName, String suiteName, Integer failCount, Double failRate) {
            this.rank = rank;
            this.caseName = caseName;
            this.suiteName = suiteName;
            this.failCount = failCount;
            this.failRate = failRate;
        }
    }

    /**
     * 缺陷趋势项（按周聚合）
     */
    @Data
    public static class DefectTrendItem {
        private String week;
        private Integer newCount;
        private Integer fixedCount;

        public DefectTrendItem() {}

        public DefectTrendItem(String week, Integer newCount, Integer fixedCount) {
            this.week = week;
            this.newCount = newCount;
            this.fixedCount = fixedCount;
        }
    }
}
