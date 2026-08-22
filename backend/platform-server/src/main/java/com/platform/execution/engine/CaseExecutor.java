/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试用例执行器
 */
package com.platform.execution.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.execution.entity.TestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 测试用例执行器
 *
 * <p>执行单个测试用例的完整生命周期：Setup → Steps → Teardown。
 * 每个阶段的步骤树从 TestCase 的 JSON 字段反序列化。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CaseExecutor {

    private final KeywordExecutor keywordExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 执行测试用例
     *
     * @param testCase 测试用例
     * @param context  执行上下文
     * @return 执行结果（status + stepLogs + durationMs）
     */
    public StepResult execute(TestCase testCase, ExecutionContext context) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> stepLogs = new ArrayList<>();
        boolean passed = true;
        String errorMessage = null;

        // 反序列化步骤树
        List<StepNode> setupSteps = parseSteps(testCase.getSetupSteps());
        List<StepNode> mainSteps = parseSteps(testCase.getSteps());
        List<StepNode> teardownSteps = parseSteps(testCase.getTeardownSteps());

        // 执行 Setup 步骤
        for (StepNode step : setupSteps) {
            StepResult sr = executeStep(step, testCase.getName(), "setup", context, stepLogs);
            if ("FAILED".equals(sr.getStatus()) || "ERROR".equals(sr.getStatus())) {
                passed = false;
                errorMessage = "Setup 步骤失败：" + sr.getMessage();
                break;
            }
        }

        // 执行主步骤
        if (passed) {
            for (StepNode step : mainSteps) {
                StepResult sr = executeStep(step, testCase.getName(), "main", context, stepLogs);
                if ("FAILED".equals(sr.getStatus()) || "ERROR".equals(sr.getStatus())) {
                    passed = false;
                    errorMessage = sr.getMessage();
                    break;
                }
            }
        }

        // 执行 Teardown 步骤（无论前面是否失败都执行）
        for (StepNode step : teardownSteps) {
            executeStep(step, testCase.getName(), "teardown", context, stepLogs);
        }

        long elapsed = System.currentTimeMillis() - start;
        StepResult result = new StepResult();
        result.setStatus(passed ? "PASSED" : "FAILED");
        result.setMessage(passed ? "用例执行通过" : (errorMessage != null ? errorMessage : "用例执行失败"));
        result.setDurationMs(elapsed);
        Map<String, Object> respDetail = new LinkedHashMap<>();
        respDetail.put("stepLogs", stepLogs);
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
            log.error("步骤执行异常: 用例={}, 步骤={}", caseName, step.getName(), e);
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
     * 解析步骤树 JSON
     */
    private List<StepNode> parseSteps(String json) {
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
