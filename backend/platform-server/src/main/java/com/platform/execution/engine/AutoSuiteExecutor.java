/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件执行器
 */
package com.platform.execution.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.execution.entity.AutoCase;
import com.platform.execution.entity.AutoSuite;
import com.platform.execution.entity.AutoSuiteCaseLifecycle;
import com.platform.execution.mapper.AutoCaseMapper;
import com.platform.execution.service.AutoSuiteCaseLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 自动化套件执行器
 *
 * <p>按 PRD 定义的九步执行模型执行自动化套件：
 * <ol>
 *   <li>套件级 Once Setup（enable_once_setup_teardown=true 时执行一次）</li>
 *   <li>对每条自动化用例循环：
 *     <ol type="a">
 *       <li>套件级 Per-Case Setup（enable_per_case_setup_teardown=true 时）</li>
 *       <li>自动化用例级 Setup（从 auto_suite_case_lifecycle 或 auto_case.setup_steps）</li>
 *       <li>自动化用例主步骤</li>
 *       <li>自动化用例级 Teardown</li>
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
 *   <li>Once Setup 失败时跳过所有自动化用例，直接执行 Once Teardown</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoSuiteExecutor {

    private final AutoCaseExecutor autoCaseExecutor;
    private final AutoCaseMapper autoCaseMapper;
    private final AutoSuiteCaseLifecycleService autoSuiteCaseLifecycleService;

    /**
     * 执行自动化套件
     *
     * @param suite   自动化套件
     * @param context 执行上下文
     * @return 套件执行结果（含每条自动化用例的执行详情）
     */
    public AutoSuiteExecutionResult execute(AutoSuite suite, ExecutionContext context) {
        // 查询套件下启用的自动化用例
        LambdaQueryWrapper<AutoCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoCase::getAutoSuiteId, suite.getId())
                .eq(AutoCase::getIsActive, true)
                .orderByAsc(AutoCase::getCreatedAt);
        List<AutoCase> cases = autoCaseMapper.selectList(wrapper);

        AutoSuiteExecutionResult result = new AutoSuiteExecutionResult();
        result.setTotal(cases.size());

        boolean onceSetupFailed = false;

        // ===== 步骤1: 套件级 Once Setup =====
        if (Integer.valueOf(1).equals(suite.getEnableOnceSetupTeardown())) {
            List<StepNode> onceSetupSteps = autoCaseExecutor.parseSteps(suite.getOnceSetupSteps());
            if (!onceSetupSteps.isEmpty()) {
                StepResult setupResult = executePhaseSteps(onceSetupSteps, "suite_once_setup", context);
                if (isFailed(setupResult)) {
                    onceSetupFailed = true;
                    log.warn("套件级 Once Setup 失败: {}", setupResult.getMessage());
                    // 标记所有自动化用例为 SKIPPED
                    for (AutoCase tc : cases) {
                        AutoCaseExecutionSummary summary = new AutoCaseExecutionSummary();
                        summary.setAutoCaseId(tc.getId());
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

        // ===== 步骤2: 对每条自动化用例循环 =====
        if (!onceSetupFailed) {
            for (AutoCase autoCase : cases) {
                AutoCaseExecutionSummary summary = executeCase(suite, autoCase, context);
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
            List<StepNode> onceTeardownSteps = autoCaseExecutor.parseSteps(suite.getOnceTeardownSteps());
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
     * 执行单条自动化用例（含套件级 Per-Case 生命周期和自动化用例级 Setup/Teardown）
     */
    private AutoCaseExecutionSummary executeCase(AutoSuite suite, AutoCase autoCase, ExecutionContext context) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> stepLogs = new ArrayList<>();
        boolean passed = true;
        String errorMessage = null;

        // ===== 步骤 2a: 套件级 Per-Case Setup =====
        if (Integer.valueOf(1).equals(suite.getEnablePerCaseSetupTeardown())) {
            List<StepNode> perCaseSetup = autoCaseExecutor.parseSteps(suite.getPerCaseSetupSteps());
            if (!perCaseSetup.isEmpty()) {
                StepResult sr = executePhaseSteps(perCaseSetup, "suite_per_case_setup", context);
                collectLogs(sr, stepLogs);
                if (isFailed(sr)) {
                    passed = false;
                    errorMessage = "套件级 Per-Case Setup 失败：" + sr.getMessage();
                }
            }
        }

        // ===== 步骤 2b: 自动化用例级 Setup（优先 auto_suite_case_lifecycle） =====
        if (passed) {
            String setupStepsJson = resolveSetupSteps(suite.getId(), autoCase);
            List<StepNode> caseSetup = autoCaseExecutor.parseSteps(setupStepsJson);
            if (!caseSetup.isEmpty()) {
                StepResult sr = executePhaseSteps(caseSetup, "case_setup", context);
                collectLogs(sr, stepLogs);
                if (isFailed(sr)) {
                    passed = false;
                    errorMessage = "自动化用例 Setup 失败：" + sr.getMessage();
                }
            }
        }

        // ===== 步骤 2c: 自动化用例主步骤 =====
        if (passed) {
            StepResult mainResult = autoCaseExecutor.executeMainSteps(autoCase, context);
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

        // ===== 步骤 2d: 自动化用例级 Teardown（优先 auto_suite_case_lifecycle） =====
        String teardownStepsJson = resolveTeardownSteps(suite.getId(), autoCase);
        List<StepNode> caseTeardown = autoCaseExecutor.parseSteps(teardownStepsJson);
        if (!caseTeardown.isEmpty()) {
            StepResult sr = executePhaseSteps(caseTeardown, "case_teardown", context);
            collectLogs(sr, stepLogs);
            // Teardown 失败不影响主结果
            if (isFailed(sr)) {
                log.warn("自动化用例级 Teardown 失败（不影响主结果）: {}", sr.getMessage());
            }
        }

        // ===== 步骤 2e: 套件级 Per-Case Teardown =====
        if (Integer.valueOf(1).equals(suite.getEnablePerCaseSetupTeardown())) {
            List<StepNode> perCaseTeardown = autoCaseExecutor.parseSteps(suite.getPerCaseTeardownSteps());
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
        AutoCaseExecutionSummary summary = new AutoCaseExecutionSummary();
        summary.setAutoCaseId(autoCase.getId());
        summary.setCaseName(autoCase.getName());
        summary.setStatus(passed ? "PASSED" : (errorMessage != null && errorMessage.contains("ERROR") ? "ERROR" : "FAILED"));
        summary.setMessage(passed ? "自动化用例执行通过" : errorMessage);
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
            lastResult = autoCaseExecutor.executeStep(step, phase, context);
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
     * 解析自动化用例的 Setup 步骤（优先 auto_suite_case_lifecycle，否则用自动化用例自身的 setup_steps）
     */
    private String resolveSetupSteps(Long autoSuiteId, AutoCase autoCase) {
        AutoSuiteCaseLifecycle lifecycle = autoSuiteCaseLifecycleService.findByAutoSuiteAndCase(autoSuiteId, autoCase.getId());
        if (lifecycle != null && lifecycle.getSetupSteps() != null && !lifecycle.getSetupSteps().trim().isEmpty()) {
            return lifecycle.getSetupSteps();
        }
        return autoCase.getSetupSteps();
    }

    /**
     * 解析自动化用例的 Teardown 步骤（优先 auto_suite_case_lifecycle，否则用自动化用例自身的 teardown_steps）
     */
    private String resolveTeardownSteps(Long autoSuiteId, AutoCase autoCase) {
        AutoSuiteCaseLifecycle lifecycle = autoSuiteCaseLifecycleService.findByAutoSuiteAndCase(autoSuiteId, autoCase.getId());
        if (lifecycle != null && lifecycle.getTeardownSteps() != null && !lifecycle.getTeardownSteps().trim().isEmpty()) {
            return lifecycle.getTeardownSteps();
        }
        return autoCase.getTeardownSteps();
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
     * 自动化套件执行结果
     */
    @lombok.Data
    public static class AutoSuiteExecutionResult {
        private int total;
        private int passed;
        private int failed;
        private int skipped;
        private List<AutoCaseExecutionSummary> caseResults = new ArrayList<>();

        public void incrementPassed() { passed++; }
        public void incrementFailed() { failed++; }
        public void incrementSkipped() { skipped++; }
    }

    /**
     * 单条自动化用例执行摘要
     */
    @lombok.Data
    public static class AutoCaseExecutionSummary {
        private Long autoCaseId;
        private String caseName;
        private String status;
        private String message;
        private long durationMs;
        private List<Map<String, Object>> stepLogs;
    }
}
