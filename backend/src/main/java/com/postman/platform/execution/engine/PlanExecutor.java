package com.postman.platform.execution.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postman.platform.execution.entity.TestExecution;
import com.postman.platform.execution.entity.TestPlan;
import com.postman.platform.execution.entity.TestResult;
import com.postman.platform.execution.entity.TestSuite;
import com.postman.platform.execution.mapper.TestExecutionMapper;
import com.postman.platform.execution.mapper.TestPlanMapper;
import com.postman.platform.execution.mapper.TestResultMapper;
import com.postman.platform.execution.mapper.TestSuiteMapper;
import com.postman.platform.execution.websocket.ExecutionWebSocketHandler;
import com.postman.platform.environment.entity.Environment;
import com.postman.platform.environment.mapper.EnvironmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 测试计划执行器
 *
 * <p>顶层执行器，协调整个执行流程：
 * <ol>
 *   <li>加载 TestExecution + TestPlan</li>
 *   <li>创建 ExecutionContext（加载环境配置）</li>
 *   <li>遍历套件 → 遍历用例 → 执行</li>
 *   <li>保存 TestResult + 更新 TestExecution 统计</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanExecutor {

    private final SuiteExecutor suiteExecutor;
    private final TestPlanMapper testPlanMapper;
    private final TestExecutionMapper testExecutionMapper;
    private final TestResultMapper testResultMapper;
    private final TestSuiteMapper testSuiteMapper;
    private final EnvironmentMapper environmentMapper;
    private final ObjectMapper objectMapper;
    private final ExecutionWebSocketHandler executionWebSocketHandler;

    /**
     * 执行测试计划
     *
     * @param executionId 执行记录 ID
     */
    public void execute(Long executionId) {
        TestExecution execution = testExecutionMapper.selectById(executionId);
        if (execution == null) {
            log.error("执行记录不存在：{}", executionId);
            return;
        }

        // 更新为 RUNNING
        execution.setStatus("RUNNING");
        execution.setStartedAt(LocalDateTime.now());
        testExecutionMapper.updateById(execution);
        sendProgress(executionId, "RUNNING", 0, 0, 0, 0, 0, "开始执行");

        long startMs = System.currentTimeMillis();
        int totalCases = 0;
        int passedCases = 0;
        int failedCases = 0;
        int skippedCases = 0;
        boolean hasError = false;

        try {
            // 查测试计划
            TestPlan plan = testPlanMapper.selectById(execution.getPlanId());
            if (plan == null) {
                throw new RuntimeException("测试计划不存在：" + execution.getPlanId());
            }

            // 创建执行上下文
            ExecutionContext context = buildContext(execution, plan);
            log.info("开始执行计划: plan={}, execution={}, env={}", plan.getName(), executionId, context.getEnvironmentId());

            // 解析 suiteIds
            List<Long> suiteIds = parseSuiteIds(plan.getSuiteIds());
            if (suiteIds.isEmpty()) {
                log.warn("计划未关联任何套件: {}", plan.getName());
            }

            // 遍历套件执行
            for (Long suiteId : suiteIds) {
                TestSuite suite = testSuiteMapper.selectById(suiteId);
                if (suite == null) {
                    log.warn("套件不存在，跳过: {}", suiteId);
                    continue;
                }
                SuiteExecutor.SuiteExecutionResult suiteResult = suiteExecutor.execute(suite, context);

                for (SuiteExecutor.CaseExecutionSummary caseSummary : suiteResult.getCaseResults()) {
                    // 保存 TestResult
                    TestResult testResult = new TestResult();
                    testResult.setExecutionId(executionId);
                    testResult.setCaseId(caseSummary.getCaseId());
                    testResult.setStatus(caseSummary.getStatus());
                    testResult.setActualResult(caseSummary.getMessage());
                    testResult.setDurationMs((int) caseSummary.getDurationMs());
                    testResult.setStartedAt(LocalDateTime.now());
                    testResult.setFinishedAt(LocalDateTime.now());
                    try {
                        testResult.setLogs(objectMapper.writeValueAsString(caseSummary.getStepLogs()));
                    } catch (Exception e) {
                        log.warn("序列化步骤日志失败: {}", e.getMessage());
                    }
                    testResultMapper.insert(testResult);
                }

                totalCases += suiteResult.getTotal();
                passedCases += suiteResult.getPassed();
                failedCases += suiteResult.getFailed();
                skippedCases += suiteResult.getSkipped();
                sendProgress(executionId, "RUNNING", totalCases, passedCases, failedCases, skippedCases,
                        (int) (System.currentTimeMillis() - startMs),
                        "套件「" + suite.getName() + "」执行完成");
            }

        } catch (Exception e) {
            log.error("执行计划异常: execution={}", executionId, e);
            hasError = true;
        }

        // 更新执行记录
        long elapsed = System.currentTimeMillis() - startMs;
        execution.setTotalCases(totalCases);
        execution.setPassedCases(passedCases);
        execution.setFailedCases(failedCases);
        execution.setSkippedCases(skippedCases);
        execution.setDurationMs((int) elapsed);
        execution.setFinishedAt(LocalDateTime.now());

        if (hasError) {
            execution.setStatus("FAILED");
        } else if (failedCases > 0) {
            execution.setStatus("COMPLETED");
        } else {
            execution.setStatus("COMPLETED");
        }

        testExecutionMapper.updateById(execution);
        sendProgress(executionId, hasError ? "FAILED" : "COMPLETED", totalCases, passedCases, failedCases,
                skippedCases, (int) elapsed, "执行完成");
        log.info("执行完成: execution={}, total={}, passed={}, failed={}, skipped={}, duration={}ms",
                executionId, totalCases, passedCases, failedCases, skippedCases, elapsed);
    }

    /**
     * 构建执行上下文
     */
    private ExecutionContext buildContext(TestExecution execution, TestPlan plan) {
        ExecutionContext context = new ExecutionContext();
        context.setExecutionId(execution.getId());
        context.setProjectId(plan.getProjectId());
        context.setEnvironmentId(execution.getEnvironmentId());

        // 加载环境配置
        Long envId = execution.getEnvironmentId() != null
                ? execution.getEnvironmentId() : plan.getEnvironmentId();
        if (envId != null) {
            Environment env = environmentMapper.selectById(envId);
            if (env != null) {
                String baseUrl = "";
                if (env.getHost() != null) {
                    baseUrl = env.getPort() != null
                            ? "http://" + env.getHost() + ":" + env.getPort()
                            : "http://" + env.getHost();
                }
                context.setBaseUrl(baseUrl);
                context.setEnvironmentId(env.getId());

                // 解析额外配置
                if (env.getConfigJson() != null && !env.getConfigJson().isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> config = objectMapper.readValue(env.getConfigJson(), Map.class);
                        context.setEnvConfig(config);
                    } catch (Exception e) {
                        log.warn("解析环境配置 JSON 失败: {}", e.getMessage());
                    }
                }
            }
        }

        return context;
    }

    /**
     * 解析套件 ID 列表
     */
    private List<Long> parseSuiteIds(String suiteIdsJson) {
        if (suiteIdsJson == null || suiteIdsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(suiteIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("解析 suiteIds 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 通过 WebSocket 推送执行进度
     */
    private void sendProgress(Long executionId, String status, int totalCases,
                              int passedCases, int failedCases, int skippedCases,
                              int durationMs, String message) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "PROGRESS");
            payload.put("status", status);
            payload.put("totalCases", totalCases);
            payload.put("passedCases", passedCases);
            payload.put("failedCases", failedCases);
            payload.put("skippedCases", skippedCases);
            payload.put("durationMs", durationMs);
            payload.put("message", message);
            executionWebSocketHandler.sendProgress(String.valueOf(executionId), objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("推送执行进度失败: {}", e.getMessage());
        }
    }
}
