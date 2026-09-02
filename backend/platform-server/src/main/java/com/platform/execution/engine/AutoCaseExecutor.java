/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化用例执行器
 */
package com.platform.execution.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.execution.entity.AutoCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 自动化用例执行器
 *
 * <p>执行单个自动化用例的完整生命周期：Setup → Steps → Teardown。
 * 每个阶段的步骤树从 AutoCase 的 JSON 字段反序列化。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCaseExecutor {

    private final KeywordExecutor keywordExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 执行自动化用例的完整生命周期（Setup → Steps → Teardown）
     *
     * <p>此方法供独立执行自动化用例调用。当由 AutoSuiteExecutor 调度时，
     * 使用 {@link #executeMainSteps} 和 {@link #executeStep} 分阶段执行。
     *
     * @param autoCase 自动化用例
     * @param context  执行上下文
     * @return 执行结果（status + stepLogs + durationMs）
     */
    public StepResult execute(AutoCase autoCase, ExecutionContext context) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> stepLogs = new ArrayList<>();
        boolean passed = true;
        String errorMessage = null;

        // 反序列化步骤树
        List<StepNode> setupSteps = parseSteps(autoCase.getSetupSteps());
        List<StepNode> mainSteps = parseSteps(autoCase.getSteps());
        List<StepNode> teardownSteps = parseSteps(autoCase.getTeardownSteps());

        // 执行 Setup 步骤
        for (StepNode step : setupSteps) {
            StepResult sr = executeStep(step, autoCase.getName(), "setup", context, stepLogs);
            if ("FAILED".equals(sr.getStatus()) || "ERROR".equals(sr.getStatus())) {
                passed = false;
                errorMessage = "Setup 步骤失败：" + sr.getMessage();
                break;
            }
        }

        // 执行主步骤
        if (passed) {
            for (StepNode step : mainSteps) {
                StepResult sr = executeStep(step, autoCase.getName(), "main", context, stepLogs);
                if ("FAILED".equals(sr.getStatus()) || "ERROR".equals(sr.getStatus())) {
                    passed = false;
                    errorMessage = sr.getMessage();
                    break;
                }
            }
        }

        // 执行 Teardown 步骤（无论前面是否失败都执行）
        for (StepNode step : teardownSteps) {
            executeStep(step, autoCase.getName(), "teardown", context, stepLogs);
        }

        return buildCaseResult(passed, errorMessage, start, stepLogs);
    }

    /**
     * 仅执行自动化用例的主步骤（不含 Setup/Teardown）
     *
     * <p>供 AutoSuiteExecutor 调度时使用。Setup/Teardown 由 AutoSuiteExecutor 管理。
     *
     * @param autoCase 自动化用例
     * @param context  执行上下文
     * @return 执行结果（status + stepLogs + durationMs）
     */
    public StepResult executeMainSteps(AutoCase autoCase, ExecutionContext context) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> stepLogs = new ArrayList<>();
        boolean passed = true;
        String errorMessage = null;

        List<StepNode> mainSteps = parseSteps(autoCase.getSteps());
        for (StepNode step : mainSteps) {
            StepResult sr = executeStep(step, autoCase.getName(), "main", context, stepLogs);
            if ("FAILED".equals(sr.getStatus()) || "ERROR".equals(sr.getStatus())) {
                passed = false;
                errorMessage = sr.getMessage();
                break;
            }
        }

        return buildCaseResult(passed, errorMessage, start, stepLogs);
    }

    /**
     * 执行单个步骤（公开方法，供 AutoSuiteExecutor 调用）
     *
     * @param step    步骤节点
     * @param phase   阶段标识（suite_once_setup / case_setup / main / case_teardown 等）
     * @param context 执行上下文
     * @return 步骤执行结果（response 中包含 stepLogs 列表）
     */
    public StepResult executeStep(StepNode step, String phase, ExecutionContext context) {
        List<Map<String, Object>> stepLogs = new ArrayList<>();
        StepResult result = executeStep(step, "", phase, context, stepLogs);

        // 将 stepLogs 附带到 response 中
        Map<String, Object> respDetail = new LinkedHashMap<>();
        respDetail.put("stepLogs", stepLogs);
        if (result.getResponse() != null) {
            respDetail.putAll(result.getResponse());
        }
        result.setResponse(respDetail);
        return result;
    }

    /**
     * 执行单个步骤并记录日志
     */
    private StepResult executeStep(StepNode step, String caseName, String phase,
                                   ExecutionContext context, List<Map<String, Object>> stepLogs) {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("stepName", step.getName());
        logEntry.put("keywordId", step.getKeywordId());
        logEntry.put("phase", phase);

        StepResult result;
        try {
            result = keywordExecutor.execute(step, context);
        } catch (Exception e) {
            log.error("步骤执行异常: 自动化用例={}, 步骤={}", caseName, step.getName(), e);
            result = StepResult.error("步骤执行异常：" + e.getMessage());
        }

        logEntry.put("status", result.getStatus());
        logEntry.put("message", result.getMessage());
        logEntry.put("durationMs", result.getDurationMs());
        if (result.getRequest() != null) {
            logEntry.put("request", result.getRequest());
        }
        if (result.getResponse() != null) {
            logEntry.put("response", result.getResponse());
        }
        if (result.getAssertionSummary() != null) {
            logEntry.put("assertionSummary", result.getAssertionSummary());
        }
        stepLogs.add(logEntry);
        context.addLog(logEntry);

        return result;
    }

    /**
     * 构建自动化用例执行结果
     */
    private StepResult buildCaseResult(boolean passed, String errorMessage, long start, List<Map<String, Object>> stepLogs) {
        long elapsed = System.currentTimeMillis() - start;
        StepResult result = new StepResult();
        result.setStatus(passed ? "PASSED" : "FAILED");
        result.setMessage(passed ? "自动化用例执行通过" : (errorMessage != null ? errorMessage : "自动化用例执行失败"));
        result.setDurationMs(elapsed);
        Map<String, Object> respDetail = new LinkedHashMap<>();
        respDetail.put("stepLogs", stepLogs);
        result.setResponse(respDetail);
        return result;
    }

    /**
     * 解析步骤树 JSON（公开方法，供 AutoSuiteExecutor 调用）
     */
    public List<StepNode> parseSteps(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<StepNode>>() {});
        } catch (Exception e) {
            log.warn("解析步骤树失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
