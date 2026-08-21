package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.PlanCreateRequest;
import com.platform.execution.dto.PlanResponse;
import com.platform.execution.dto.PlanUpdateRequest;
import com.platform.execution.entity.TestPlan;
import com.platform.execution.mapper.TestPlanMapper;
import com.platform.environment.entity.Environment;
import com.platform.environment.mapper.EnvironmentMapper;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 测试计划服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final TestPlanMapper testPlanMapper;
    private final ProjectService projectService;
    private final EnvironmentMapper environmentMapper;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询测试计划
     */
    public PageResponse<PlanResponse> listPlans(Long projectId, String keyword,
                                                 int page, int pageSize) {
        LambdaQueryWrapper<TestPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestPlan::getProjectId, projectId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TestPlan::getName, keyword)
                    .or().like(TestPlan::getDescription, keyword));
        }
        wrapper.orderByDesc(TestPlan::getCreatedAt);

        Page<TestPlan> result = testPlanMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<PlanResponse> records = new ArrayList<>(result.getRecords().size());
        for (TestPlan p : result.getRecords()) {
            records.add(toResponse(p));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 获取计划详情
     */
    public PlanResponse getPlan(Long planId) {
        return toResponse(findById(planId));
    }

    /**
     * 创建测试计划
     */
    @Transactional(rollbackFor = Exception.class)
    public PlanResponse createPlan(PlanCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        // 名称唯一性检查
        LambdaQueryWrapper<TestPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestPlan::getProjectId, request.getProjectId())
                .eq(TestPlan::getName, request.getName());
        if (testPlanMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND, "计划名称已存在：" + request.getName());
        }

        TestPlan plan = new TestPlan();
        BeanUtils.copyProperties(request, plan);
        plan.setSuiteIds(serializeSuiteIds(request.getSuiteIds()));
        plan.setIsActive(1);
        plan.setCreatedBy(getCurrentUserId());
        testPlanMapper.insert(plan);
        return toResponse(plan);
    }

    /**
     * 更新测试计划
     */
    @Transactional(rollbackFor = Exception.class)
    public PlanResponse updatePlan(Long planId, PlanUpdateRequest request) {
        TestPlan plan = findById(planId);

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(plan.getName())) {
            LambdaQueryWrapper<TestPlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TestPlan::getProjectId, plan.getProjectId())
                    .eq(TestPlan::getName, request.getName())
                    .ne(TestPlan::getId, planId);
            if (testPlanMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.PLAN_NOT_FOUND, "计划名称已存在：" + request.getName());
            }
            plan.setName(request.getName());
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getSuiteIds() != null) {
            plan.setSuiteIds(serializeSuiteIds(request.getSuiteIds()));
        }
        if (request.getEnvironmentId() != null) {
            plan.setEnvironmentId(request.getEnvironmentId());
        }
        if (request.getScheduleCron() != null) {
            plan.setScheduleCron(request.getScheduleCron());
        }
        if (request.getIsActive() != null) {
            plan.setIsActive(request.getIsActive());
        }

        testPlanMapper.updateById(plan);
        return toResponse(plan);
    }

    /**
     * 删除测试计划
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long planId) {
        findById(planId);
        testPlanMapper.deleteById(planId);
    }

    private TestPlan findById(Long planId) {
        TestPlan plan = testPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND, "测试计划不存在：" + planId);
        }
        return plan;
    }

    private PlanResponse toResponse(TestPlan plan) {
        PlanResponse resp = new PlanResponse();
        BeanUtils.copyProperties(plan, resp);
        resp.setSuiteIds(parseSuiteIds(plan.getSuiteIds()));
        // 获取环境名称
        if (plan.getEnvironmentId() != null) {
            Environment env = environmentMapper.selectById(plan.getEnvironmentId());
            if (env != null) {
                resp.setEnvironmentName(env.getName());
            }
        }
        return resp;
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseSuiteIds(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String serializeSuiteIds(List<Long> suiteIds) {
        if (suiteIds == null || suiteIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(suiteIds);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "序列化 suiteIds 失败");
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }
}
