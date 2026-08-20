package com.postman.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.postman.platform.auth.entity.User;
import com.postman.platform.common.exception.BusinessException;
import com.postman.platform.common.exception.ErrorCode;
import com.postman.platform.common.response.PageResponse;
import com.postman.platform.execution.dto.ExecutionResponse;
import com.postman.platform.execution.dto.ExecutionStartRequest;
import com.postman.platform.execution.dto.TestResultResponse;
import com.postman.platform.execution.entity.TestExecution;
import com.postman.platform.execution.entity.TestPlan;
import com.postman.platform.execution.entity.TestResult;
import com.postman.platform.execution.mapper.TestExecutionMapper;
import com.postman.platform.execution.mapper.TestPlanMapper;
import com.postman.platform.execution.mapper.TestResultMapper;
import com.postman.platform.execution.mq.ExecutionMessage;
import com.postman.platform.execution.mq.ExecutionProducer;
import com.postman.platform.execution.entity.TestCase;
import com.postman.platform.execution.mapper.TestCaseMapper;
import com.postman.platform.environment.entity.Environment;
import com.postman.platform.environment.mapper.EnvironmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试执行服务
 *
 * <p>负责执行记录的查询、触发执行和取消。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionService {

    private final TestExecutionMapper testExecutionMapper;
    private final TestPlanMapper testPlanMapper;
    private final TestResultMapper testResultMapper;
    private final TestCaseMapper testCaseMapper;
    private final EnvironmentMapper environmentMapper;
    private final ExecutionProducer executionProducer;

    /**
     * 分页查询项目下的执行记录
     */
    public PageResponse<ExecutionResponse> listExecutions(String projectId, String status,
                                                          int page, int pageSize) {
        // 先查项目下的 planIds
        LambdaQueryWrapper<TestPlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(TestPlan::getProjectId, projectId)
                .select(TestPlan::getId);
        List<TestPlan> plans = testPlanMapper.selectList(planWrapper);
        List<String> planIds = plans.stream().map(TestPlan::getId).collect(Collectors.toList());

        if (planIds.isEmpty()) {
            return PageResponse.empty((long) page, (long) pageSize);
        }

        LambdaQueryWrapper<TestExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TestExecution::getPlanId, planIds);
        if (StringUtils.hasText(status)) {
            wrapper.eq(TestExecution::getStatus, status);
        }
        wrapper.orderByDesc(TestExecution::getCreatedAt);

        Page<TestExecution> result = testExecutionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<ExecutionResponse> records = new ArrayList<>(result.getRecords().size());
        for (TestExecution e : result.getRecords()) {
            records.add(toResponse(e));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 获取执行详情
     */
    public ExecutionResponse getExecution(String executionId) {
        TestExecution execution = testExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BusinessException(ErrorCode.EXECUTION_NOT_FOUND, "执行记录不存在：" + executionId);
        }
        return toResponse(execution);
    }

    /**
     * 获取执行结果明细
     */
    public List<TestResultResponse> getResults(String executionId) {
        LambdaQueryWrapper<TestResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestResult::getExecutionId, executionId)
                .orderByAsc(TestResult::getStartedAt);
        List<TestResult> results = testResultMapper.selectList(wrapper);

        List<TestResultResponse> records = new ArrayList<>(results.size());
        for (TestResult r : results) {
            records.add(toResultResponse(r));
        }
        return records;
    }

    /**
     * 触发执行
     */
    @Transactional(rollbackFor = Exception.class)
    public ExecutionResponse startExecution(String planId, ExecutionStartRequest request) {
        TestPlan plan = testPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND, "测试计划不存在：" + planId);
        }

        // 创建执行记录
        TestExecution execution = new TestExecution();
        execution.setPlanId(planId);
        execution.setEnvironmentId(request.getEnvironmentId() != null
                ? request.getEnvironmentId() : plan.getEnvironmentId());
        execution.setTriggerType(request.getTriggerType() != null
                ? request.getTriggerType() : "MANUAL");
        execution.setStatus("PENDING");
        execution.setTotalCases(0);
        execution.setPassedCases(0);
        execution.setFailedCases(0);
        execution.setSkippedCases(0);
        execution.setTriggeredBy(getCurrentUserId());
        execution.setCreatedAt(LocalDateTime.now());
        testExecutionMapper.insert(execution);

        // 发送 MQ 消息异步执行
        ExecutionMessage message = new ExecutionMessage();
        message.setExecutionId(execution.getId());
        message.setPlanId(planId);
        message.setEnvironmentId(execution.getEnvironmentId());
        message.setTriggeredBy(execution.getTriggeredBy());
        message.setTriggerType(execution.getTriggerType());
        executionProducer.sendExecutionMessage(message);

        log.info("触发执行: planId={}, executionId={}", planId, execution.getId());
        return toResponse(execution);
    }

    /**
     * 取消执行
     */
    @Transactional(rollbackFor = Exception.class)
    public ExecutionResponse cancelExecution(String executionId) {
        TestExecution execution = testExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BusinessException(ErrorCode.EXECUTION_NOT_FOUND, "执行记录不存在：" + executionId);
        }

        if (!"PENDING".equals(execution.getStatus()) && !"RUNNING".equals(execution.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "只能取消 PENDING 或 RUNNING 状态的执行");
        }

        execution.setStatus("CANCELLED");
        execution.setFinishedAt(LocalDateTime.now());
        testExecutionMapper.updateById(execution);
        return toResponse(execution);
    }

    private ExecutionResponse toResponse(TestExecution execution) {
        ExecutionResponse resp = new ExecutionResponse();
        BeanUtils.copyProperties(execution, resp);

        // 获取计划名称
        TestPlan plan = testPlanMapper.selectById(execution.getPlanId());
        if (plan != null) {
            resp.setPlanName(plan.getName());
        }

        // 获取环境名称
        if (execution.getEnvironmentId() != null) {
            Environment env = environmentMapper.selectById(execution.getEnvironmentId());
            if (env != null) {
                resp.setEnvironmentName(env.getName());
            }
        }

        return resp;
    }

    private TestResultResponse toResultResponse(TestResult result) {
        TestResultResponse resp = new TestResultResponse();
        BeanUtils.copyProperties(result, resp);

        // 获取用例名称
        TestCase testCase = testCaseMapper.selectById(result.getCaseId());
        if (testCase != null) {
            resp.setCaseName(testCase.getName());
        }

        return resp;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }
}
