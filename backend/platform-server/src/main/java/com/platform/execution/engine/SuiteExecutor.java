/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试套件执行器
 */
package com.platform.execution.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.execution.entity.TestCase;
import com.platform.execution.entity.TestSuite;
import com.platform.execution.mapper.TestCaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 测试套件执行器
 *
 * <p>执行套件下所有启用的测试用例，汇总统计结果。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuiteExecutor {

    private final CaseExecutor caseExecutor;
    private final TestCaseMapper testCaseMapper;

    /**
     * 执行测试套件
     *
     * @param suite   测试套件
     * @param context 执行上下文
     * @return 套件执行结果（含每条用例的执行详情）
     */
    public SuiteExecutionResult execute(TestSuite suite, ExecutionContext context) {
        // 查询套件下启用的用例
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestCase::getSuiteId, suite.getId())
                .eq(TestCase::getIsActive, true)
                .orderByAsc(TestCase::getCreatedAt);
        List<TestCase> cases = testCaseMapper.selectList(wrapper);

        SuiteExecutionResult result = new SuiteExecutionResult();
        result.setTotal(cases.size());

        for (TestCase testCase : cases) {
            StepResult caseResult = caseExecutor.execute(testCase, context);

            CaseExecutionSummary summary = new CaseExecutionSummary();
            summary.setCaseId(testCase.getId());
            summary.setCaseName(testCase.getName());
            summary.setStatus(caseResult.getStatus());
            summary.setMessage(caseResult.getMessage());
            summary.setDurationMs(caseResult.getDurationMs());
            if (caseResult.getResponse() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> logs = (List<Map<String, Object>>) caseResult.getResponse().get("stepLogs");
                summary.setStepLogs(logs != null ? logs : Collections.emptyList());
            }
            result.getCaseResults().add(summary);

            switch (caseResult.getStatus()) {
                case "PASSED":
                    result.incrementPassed();
                    break;
                case "FAILED":
                    result.incrementFailed();
                    break;
                case "ERROR":
                    result.incrementFailed();
                    break;
                default:
                    result.incrementSkipped();
            }
        }

        return result;
    }

    /**
     * 套件执行结果
     */
    @lombok.Data
    public static class SuiteExecutionResult {
        private int total;
        private int passed;
        private int failed;
        private int skipped;
        private List<CaseExecutionSummary> caseResults = new ArrayList<>();

        public void incrementPassed() { passed++; }
        public void incrementFailed() { failed++; }
        public void incrementSkipped() { skipped++; }
    }

    /**
     * 单条用例执行摘要
     */
    @lombok.Data
    public static class CaseExecutionSummary {
        private Long caseId;
        private String caseName;
        private String status;
        private String message;
        private long durationMs;
        private List<Map<String, Object>> stepLogs;
    }
}
