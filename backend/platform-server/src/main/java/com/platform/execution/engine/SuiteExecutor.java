/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试套件执行器
 */
package com.platform.execution.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.execution.entity.SuiteCaseLifecycle;
import com.platform.execution.entity.TestCase;
import com.platform.execution.entity.TestSuite;
import com.platform.execution.mapper.TestCaseMapper;
import com.platform.execution.service.SuiteCaseLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 测试套件执行器
 *
 * <p>按 PRD 定义的九步执行模型执行套件：
 * <ol>
 *   <li>套件级 Once Setup（enable_once_setup_teardown=true 时执行一次）</li>
 *   <li>对每条用例循环：
 *     <ol type="a">
 *       <li>套件级 Per-Case Setup（enable_per_case_setup_teardown=true 时）</li>
 *       <li>用例级 Setup（从 suite_case_lifecycle 或 test_case.setup_steps）</li>
 *       <li>用例主步骤</li>
 *       <li>用例级 Teardown</li>
 *       <li>套件级 Per-Case Teardown</li>
 *     </ol>
 *   </li>
 *   <li>套件级 Once Teardown</li>
 * </ol>
 *
 * <p>规则：
 * <ul>
 *   <li>Setup 失败时标记 ERROR 并跳过主步骤</li>
 *   <li>Teardown 失败不影响主结果</li>
 *   <li>Once Setup 失败时跳过所有用例，直接执行 Once Teardown</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuiteExecutor {

    private final CaseExecutor caseExecutor;
    private final TestCaseMapper testCaseMapper;
    private final SuiteCaseLifecycleService suiteCaseLifecycleService;

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

        boolean onceSetupFailed = false;

        // ===== 步骤1: 套件级 Once Setup =====
        if (Integer.valueOf(1).equals(suite.getEnableOnceSetupTeardown())) {
            List<StepNode> onceSetupSteps = caseExecutor.parseSteps(suite.getOnceSetupSteps());
            if (!onceSetupSteps.isEmpty()) {
                StepResult setupResult = executePhaseSteps(onceSetupSteps, "suite_once_setup", context);
                if (isFailed(setupResult)) {
                    onceSetupFailed = true;
                    log.warn("套件级 Once Setup 失败: {}", setupResult.getMessage());
                    // 标记所有用例为 SKIPPED
                    for (TestCase tc : cases) {
                        CaseExecutionSummary summary = new CaseExecutionSummary();
                        summary.setCaseId(tc.getId());
                        summary.setCaseName(tc.getName());
                        summary.setStatus("SKIPPED");
                        summary.setMessage("套件级 Once Setup 失败，跳过执行: " + setupResult.getMessage());
                        summary.setDurationMs(0);
                        summary.setStepLogs(Collections.emptyList());
                        result.getCaseResults().add(summary);
                        result.incrementSkipped();
                    }
                }
            }
        }

        // ===== 步骤2: 对每条用例循环 =====
        if (!onceSetupFailed) {
            for (TestCase testCase : cases) {
                CaseExecutionSummary summary = executeCase(suite, testCase, context);
                result.getCaseResults().add(summary);

                switch (summary.getStatus()) {
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
        }

        // ===== 步骤3: 套件级 Once Teardown =====
        if (Integer.valueOf(1).equals(suite.getEnableOnceSetupTeardown())) {
            List<StepNode> onceTeardownSteps = caseExecutor.parseSteps(suite.getOnceTeardownSteps());
            if (!onceTeardownSteps.isEmpty()) {
                StepResult teardownResult = executePhaseSteps(onceTeardownSteps, "suite_once_teardown", context);
                if (isFailed(teardownResult)) {
                    log.warn("套件级 Once Teardown 失败: {}", teardownResult.getMessage());
                    // Teardown 失败不影响主结果
                }
            }
        }

        return result;
    }

    /**
     * 执行单条用例（含套件级 Per-Case 生命周期和用例级 Setup/Teardown）
     */
    private CaseExecutionSummary executeCase(TestSuite suite, TestCase testCase, ExecutionContext context) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> stepLogs = new ArrayList<>();
        boolean passed = true;
        String errorMessage = null;

        // ===== 步骤 2a: 套件级 Per-Case Setup =====
        if (Integer.valueOf(1).equals(suite.getEnablePerCaseSetupTeardown())) {
            List<StepNode> perCaseSetup = caseExecutor.parseSteps(suite.getPerCaseSetupSteps());
            if (!perCaseSetup.isEmpty()) {
                StepResult sr = executePhaseSteps(perCaseSetup, "suite_per_case_setup", context);
                collectLogs(sr, stepLogs);
                if (isFailed(sr)) {
                    passed = false;
                    errorMessage = "套件级 Per-Case Setup 失败：" + sr.getMessage();
                }
            }
        }

        // ===== 步骤 2b: 用例级 Setup（优先 suite_case_lifecycle） =====
        if (passed) {
            String setupStepsJson = resolveSetupSteps(suite.getId(), testCase);
            List<StepNode> caseSetup = caseExecutor.parseSteps(setupStepsJson);
            if (!caseSetup.isEmpty()) {
                StepResult sr = executePhaseSteps(caseSetup, "case_setup", context);
                collectLogs(sr, stepLogs);
                if (isFailed(sr)) {
                    passed = false;
                    errorMessage = "用例 Setup 失败：" + sr.getMessage();
                }
            }
        }

        // ===== 步骤 2c: 用例主步骤 =====
        if (passed) {
            StepResult mainResult = caseExecutor.executeMainSteps(testCase, context);
            if (mainResult.getResponse() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> logs = (List<Map<String, Object>>) mainResult.getResponse().get("stepLogs");
                if (logs != null) {
                    stepLogs.addAll(logs);
                }
            }
            if (isFailed(mainResult)) {
                passed = false;
                errorMessage = mainResult.getMessage();
            }
        }

        // ===== 步骤 2d: 用例级 Teardown（优先 suite_case_lifecycle） =====
        String teardownStepsJson = resolveTeardownSteps(suite.getId(), testCase);
        List<StepNode> caseTeardown = caseExecutor.parseSteps(teardownStepsJson);
        if (!caseTeardown.isEmpty()) {
            StepResult sr = executePhaseSteps(caseTeardown, "case_teardown", context);
            collectLogs(sr, stepLogs);
            // Teardown 失败不影响主结果
            if (isFailed(sr)) {
                log.warn("用例级 Teardown 失败（不影响主结果）: {}", sr.getMessage());
            }
        }

        // ===== 步骤 2e: 套件级 Per-Case Teardown =====
        if (Integer.valueOf(1).equals(suite.getEnablePerCaseSetupTeardown())) {
            List<StepNode> perCaseTeardown = caseExecutor.parseSteps(suite.getPerCaseTeardownSteps());
            if (!perCaseTeardown.isEmpty()) {
                StepResult sr = executePhaseSteps(perCaseTeardown, "suite_per_case_teardown", context);
                collectLogs(sr, stepLogs);
                // Teardown 失败不影响主结果
                if (isFailed(sr)) {
                    log.warn("套件级 Per-Case Teardown 失败（不影响主结果）: {}", sr.getMessage());
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        CaseExecutionSummary summary = new CaseExecutionSummary();
        summary.setCaseId(testCase.getId());
        summary.setCaseName(testCase.getName());
        summary.setStatus(passed ? "PASSED" : (errorMessage != null && errorMessage.contains("ERROR") ? "ERROR" : "FAILED"));
        summary.setMessage(passed ? "用例执行通过" : errorMessage);
        summary.setDurationMs(elapsed);
        summary.setStepLogs(stepLogs);
        return summary;
    }

    /**
     * 执行某个阶段的所有步骤，汇总所有步骤日志
     */
    private StepResult executePhaseSteps(List<StepNode> steps, String phase, ExecutionContext context) {
        List<Map<String, Object>> allLogs = new ArrayList<>();
        StepResult lastResult = null;
        for (StepNode step : steps) {
            lastResult = caseExecutor.executeStep(step, phase, context);
            // 收集每个步骤的日志
            if (lastResult.getResponse() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> logs = (List<Map<String, Object>>) lastResult.getResponse().get("stepLogs");
                if (logs != null) {
                    allLogs.addAll(logs);
                }
            }
            if (isFailed(lastResult)) {
                break;
            }
        }
        // 将汇总日志附带到结果中
        if (lastResult == null) {
            lastResult = StepResult.passed("无步骤");
        }
        Map<String, Object> respDetail = new LinkedHashMap<>();
        if (lastResult.getResponse() != null) {
            respDetail.putAll(lastResult.getResponse());
        }
        respDetail.put("stepLogs", allLogs);
        lastResult.setResponse(respDetail);
        return lastResult;
    }

    /**
     * 解析用例的 Setup 步骤（优先 suite_case_lifecycle，否则用用例自身的 setup_steps）
     */
    private String resolveSetupSteps(Long suiteId, TestCase testCase) {
        SuiteCaseLifecycle lifecycle = suiteCaseLifecycleService.findBySuiteAndCase(suiteId, testCase.getId());
        if (lifecycle != null && lifecycle.getSetupSteps() != null && !lifecycle.getSetupSteps().trim().isEmpty()) {
            return lifecycle.getSetupSteps();
        }
        return testCase.getSetupSteps();
    }

    /**
     * 解析用例的 Teardown 步骤（优先 suite_case_lifecycle，否则用用例自身的 teardown_steps）
     */
    private String resolveTeardownSteps(Long suiteId, TestCase testCase) {
        SuiteCaseLifecycle lifecycle = suiteCaseLifecycleService.findBySuiteAndCase(suiteId, testCase.getId());
        if (lifecycle != null && lifecycle.getTeardownSteps() != null && !lifecycle.getTeardownSteps().trim().isEmpty()) {
            return lifecycle.getTeardownSteps();
        }
        return testCase.getTeardownSteps();
    }

    private boolean isFailed(StepResult result) {
        if (result == null) return false;
        return "FAILED".equals(result.getStatus()) || "ERROR".equals(result.getStatus());
    }

    private void collectLogs(StepResult result, List<Map<String, Object>> stepLogs) {
        if (result != null && result.getResponse() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> logs = (List<Map<String, Object>>) result.getResponse().get("stepLogs");
            if (logs != null) {
                stepLogs.addAll(logs);
            }
        }
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
