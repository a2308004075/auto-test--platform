/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试计划执行器
 */
package com.platform.execution.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.execution.entity.TestExecution;
import com.platform.execution.entity.TestPlan;
import com.platform.execution.entity.TestResult;
import com.platform.execution.entity.TestSuite;
import com.platform.execution.entity.TestCase;
import com.platform.execution.mapper.TestExecutionMapper;
import com.platform.execution.mapper.TestPlanMapper;
import com.platform.execution.mapper.TestResultMapper;
import com.platform.execution.mapper.TestSuiteMapper;
import com.platform.execution.mapper.TestCaseMapper;
import com.platform.execution.websocket.ExecutionWebSocketHandler;
import com.platform.environment.service.EnvironmentService;
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
    private final TestCaseMapper testCaseMapper;
    private final EnvironmentService environmentService;
    private final ObjectMapper objectMapper;
    private final ExecutionWebSocketHandler executionWebSocketHandler;

    /**
     * 执行测试计划
     *
     * <p>执行流程：
     * <ol>
     *   <li>更新状态为 RUNNING</li>
     *   <li>预计算总用例数（用于进度百分比）</li>
     *   <li>遍历套件，推送 suite_start / per-case / suite_end 事件</li>
     *   <li>汇总统计并推送 plan_end 事件</li>
     * </ol>
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
        sendProgress(executionId, "RUNNING", 0, 0, 0, 0, 0, 0, 0.0, null, "开始执行");

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

            // 预计算总用例数（用于进度百分比）
            int expectedTotal = countExpectedCases(suiteIds);

            // 遍历套件执行
            for (Long suiteId : suiteIds) {
                TestSuite suite = testSuiteMapper.selectById(suiteId);
                if (suite == null) {
                    log.warn("套件不存在，跳过: {}", suiteId);
                    continue;
                }

                // 推送套件开始事件
                sendProgress(executionId, "RUNNING", totalCases, passedCases, failedCases, skippedCases,
                        (int) (System.currentTimeMillis() - startMs),
                        calcPercent(totalCases, expectedTotal),
                        calcPassRate(passedCases, totalCases),
                        null, "开始执行套件「" + suite.getName() + "」");

                SuiteExecutor.SuiteExecutionResult suiteResult = suiteExecutor.execute(suite, context);

                // 处理套件结果，逐条保存并推送进度
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

                    // 累计统计
                    totalCases++;
                    switch (caseSummary.getStatus()) {
                        case "PASSED":
                            passedCases++;
                            break;
                        case "FAILED":
                        case "ERROR":
                            failedCases++;
                            break;
                        default:
                            skippedCases++;
                    }

                    // 推送每条用例完成事件
                    sendProgress(executionId, "RUNNING", totalCases, passedCases, failedCases, skippedCases,
                            (int) (System.currentTimeMillis() - startMs),
                            calcPercent(totalCases, expectedTotal),
                            calcPassRate(passedCases, totalCases),
                            caseSummary.getCaseName(),
                            "用例「" + caseSummary.getCaseName() + "」" + caseSummary.getStatus());
                }

                // 推送套件完成事件
                sendProgress(executionId, "RUNNING", totalCases, passedCases, failedCases, skippedCases,
                        (int) (System.currentTimeMillis() - startMs),
                        calcPercent(totalCases, expectedTotal),
                        calcPassRate(passedCases, totalCases),
                        null, "套件「" + suite.getName() + "」执行完成");
            }

        } catch (Throwable e) {
            // 刻意捕获 Throwable：Error 场景也要将执行记录置为 FAILED 并推送完成事件，
            // 保证执行状态闭环，避免前端进度条永久停留在 RUNNING
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
        execution.setStatus(hasError ? "FAILED" : "COMPLETED");
        testExecutionMapper.updateById(execution);

        // 推送计划完成事件
        sendProgress(executionId, hasError ? "FAILED" : "COMPLETED", totalCases, passedCases, failedCases,
                skippedCases, (int) elapsed, 100, calcPassRate(passedCases, totalCases),
                null, "执行完成");
        log.info("执行完成: execution={}, total={}, passed={}, failed={}, skipped={}, duration={}ms",
                executionId, totalCases, passedCases, failedCases, skippedCases, elapsed);
    }

    /**
     * 预计算所有套件下启用的用例总数
     */
    private int countExpectedCases(List<Long> suiteIds) {
        int total = 0;
        for (Long suiteId : suiteIds) {
            LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TestCase::getSuiteId, suiteId)
                    .eq(TestCase::getIsActive, true);
            total += testCaseMapper.selectCount(wrapper);
        }
        return total;
    }

    /**
     * 计算进度百分比
     */
    private int calcPercent(int processed, int total) {
        if (total <= 0) return 0;
        return Math.min(100, (int) ((long) processed * 100 / total));
    }

    /**
     * 计算通过率
     */
    private double calcPassRate(int passed, int total) {
        if (total <= 0) return 0.0;
        return Math.round(passed * 1000.0 / total) / 10.0;
    }

    /**
     * 构建执行上下文
     */
    private ExecutionContext buildContext(TestExecution execution, TestPlan plan) {
        ExecutionContext context = new ExecutionContext();
        context.setExecutionId(execution.getId());
        context.setProjectId(plan.getProjectId());
        context.setEnvironmentId(execution.getEnvironmentId());

        // 加载环境变量
        Long envId = execution.getEnvironmentId() != null
                ? execution.getEnvironmentId() : plan.getEnvironmentId();
        if (envId != null) {
            context.setEnvironmentId(envId);
            try {
                Map<String, String> variables = environmentService.getVariablesAsMap(envId);

                // 从变量中构建 baseUrl（查找 host 变量）
                String host = variables.get("host");
                if (host != null && !host.isEmpty()) {
                    context.setBaseUrl(host);
                }

                // 将所有环境变量加载到执行上下文中，支持 ${var} 引用
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                log.warn("加载环境变量失败: {}", e.getMessage());
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
     *
     * @param executionId      执行记录 ID
     * @param status           执行状态
     * @param totalCases       总用例数
     * @param passedCases      通过数
     * @param failedCases      失败数
     * @param skippedCases     跳过数
     * @param durationMs       耗时（毫秒）
     * @param progressPercent  进度百分比（0-100）
     * @param passRate         通过率（0-100，保留1位小数）
     * @param currentCaseName  当前用例名称
     * @param message          消息
     */
    private void sendProgress(Long executionId, String status, int totalCases,
                              int passedCases, int failedCases, int skippedCases,
                              int durationMs, int progressPercent, double passRate,
                              String currentCaseName, String message) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "PROGRESS");
            payload.put("status", status);
            payload.put("totalCases", totalCases);
            payload.put("passedCases", passedCases);
            payload.put("failedCases", failedCases);
            payload.put("skippedCases", skippedCases);
            payload.put("durationMs", durationMs);
            payload.put("progressPercent", progressPercent);
            payload.put("passRate", passRate);
            if (currentCaseName != null) {
                payload.put("currentCaseName", currentCaseName);
            }
            payload.put("message", message);
            executionWebSocketHandler.sendProgress(String.valueOf(executionId), objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("推送执行进度失败: {}", e.getMessage());
        }
    }
}
