/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试计划管理服务
 */
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
import com.platform.execution.entity.*;
import com.platform.execution.mapper.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final PlanGroupMapper planGroupMapper;
    private final ProjectService projectService;
    private final EnvironmentMapper environmentMapper;
    private final TestExecutionMapper testExecutionMapper;
    private final AutoSuiteMapper autoSuiteMapper;
    private final AutoCaseMapper autoCaseMapper;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询测试计划
     *
     * @param groupId       分组 ID（null=不过滤，0=未分组，其他=指定分组含子分组）
     * @param triggerType   触发方式（null=不过滤）
     * @param environmentId 环境 ID（null=不过滤）
     * @param status        状态 1=启用 0=禁用（null=不过滤）
     * @param updateBegin   更新日期起（yyyy-MM-dd，null=不过滤）
     * @param updateEnd     更新日期止（yyyy-MM-dd，null=不过滤）
     * @param suiteKeyword  关联自动化套件名称关键字（null=不过滤，按项目下自动化套件名称模糊匹配）
     */
    public PageResponse<PlanResponse> listPlans(Long projectId, String keyword,
                                                 Long groupId, String triggerType,
                                                 Long environmentId, Integer status,
                                                 String updateBegin, String updateEnd,
                                                 String suiteKeyword,
                                                 int page, int pageSize) {
        LambdaQueryWrapper<TestPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestPlan::getProjectId, projectId);

        // 按分组过滤
        if (groupId != null) {
            if (groupId == 0L) {
                // 未分组
                wrapper.isNull(TestPlan::getGroupId);
            } else {
                // 指定分组（含子分组递归）
                List<Long> groupIds = getDescendantGroupIds(groupId);
                wrapper.in(TestPlan::getGroupId, groupIds);
            }
        }

        // 按关联自动化套件名称过滤：先查项目下名称匹配的自动化套件 ID，再匹配 auto_suite_ids JSON 数组
        if (StringUtils.hasText(suiteKeyword)) {
            LambdaQueryWrapper<AutoSuite> suiteWrapper = new LambdaQueryWrapper<>();
            suiteWrapper.eq(AutoSuite::getProjectId, projectId)
                    .like(AutoSuite::getName, suiteKeyword)
                    .select(AutoSuite::getId);
            List<Long> matchedSuiteIds = new ArrayList<>();
            for (AutoSuite s : autoSuiteMapper.selectList(suiteWrapper)) {
                matchedSuiteIds.add(s.getId());
            }
            if (matchedSuiteIds.isEmpty()) {
                return PageResponse.empty((long) page, (long) pageSize);
            }
            wrapper.and(w -> {
                boolean first = true;
                for (Long suiteId : matchedSuiteIds) {
                    if (first) {
                        w.apply("JSON_CONTAINS(auto_suite_ids, {0})", String.valueOf(suiteId));
                        first = false;
                    } else {
                        w.or().apply("JSON_CONTAINS(auto_suite_ids, {0})", String.valueOf(suiteId));
                    }
                }
            });
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TestPlan::getName, keyword)
                    .or().like(TestPlan::getDescription, keyword));
        }

        // 按触发方式过滤
        if (StringUtils.hasText(triggerType)) {
            wrapper.eq(TestPlan::getTriggerType, triggerType);
        }

        // 按环境 ID 过滤
        if (environmentId != null) {
            wrapper.eq(TestPlan::getEnvironmentId, environmentId);
        }

        // 按状态过滤
        if (status != null) {
            wrapper.eq(TestPlan::getIsActive, status);
        }

        // 按更新日期范围过滤
        if (StringUtils.hasText(updateBegin)) {
            try {
                wrapper.ge(TestPlan::getUpdatedAt, LocalDate.parse(updateBegin).atStartOfDay());
            } catch (Exception ignored) { /* 忽略无效日期格式 */ }
        }
        if (StringUtils.hasText(updateEnd)) {
            try {
                wrapper.le(TestPlan::getUpdatedAt, LocalDate.parse(updateEnd).atTime(23, 59, 59));
            } catch (Exception ignored) { /* 忽略无效日期格式 */ }
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
        plan.setAutoSuiteIds(serializeAutoSuiteIds(request.getAutoSuiteIds()));
        plan.setTriggerType(request.getTriggerType() != null ? request.getTriggerType() : "MANUAL");
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
        // 分组处理：clearGroup=true 时置空（归入未分组），否则有值则更新
        if (Boolean.TRUE.equals(request.getClearGroup())) {
            plan.setGroupId(null);
        } else if (request.getGroupId() != null) {
            plan.setGroupId(request.getGroupId());
        }
        if (request.getAutoSuiteIds() != null) {
            plan.setAutoSuiteIds(serializeAutoSuiteIds(request.getAutoSuiteIds()));
        }
        if (request.getEnvironmentId() != null) {
            plan.setEnvironmentId(request.getEnvironmentId());
        }
        if (request.getScheduleCron() != null) {
            plan.setScheduleCron(request.getScheduleCron());
        }
        if (request.getTriggerType() != null) {
            plan.setTriggerType(request.getTriggerType());
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

    /**
     * 清空分组及其子孙分组中的所有计划（执行记录由外键级联删除）
     *
     * @param groupId 分组 ID（0 表示未分组）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByGroup(Long projectId, Long groupId) {
        LambdaQueryWrapper<TestPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestPlan::getProjectId, projectId);
        if (groupId == 0L) {
            // 未分组
            wrapper.isNull(TestPlan::getGroupId);
        } else {
            // 指定分组（含子孙分组递归）
            wrapper.in(TestPlan::getGroupId, getDescendantGroupIds(groupId));
        }
        testPlanMapper.delete(wrapper);
    }

    /**
     * 清空项目下所有计划
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByProject(Long projectId) {
        LambdaQueryWrapper<TestPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestPlan::getProjectId, projectId);
        testPlanMapper.delete(wrapper);
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
        resp.setAutoSuiteIds(parseAutoSuiteIds(plan.getAutoSuiteIds()));

        // 获取环境名称
        if (plan.getEnvironmentId() != null) {
            Environment env = environmentMapper.selectById(plan.getEnvironmentId());
            if (env != null) {
                resp.setEnvironmentName(env.getName());
            }
        }

        // 获取自动化套件名称列表
        List<Long> autoSuiteIdList = resp.getAutoSuiteIds();
        List<String> autoSuiteNames = new ArrayList<>();
        int caseCount = 0;
        for (Long autoSuiteId : autoSuiteIdList) {
            AutoSuite suite = autoSuiteMapper.selectById(autoSuiteId);
            if (suite != null) {
                autoSuiteNames.add(suite.getName());
                // 统计该自动化套件下启用的自动化用例数
                LambdaQueryWrapper<AutoCase> caseWrapper = new LambdaQueryWrapper<>();
                caseWrapper.eq(AutoCase::getAutoSuiteId, autoSuiteId)
                        .eq(AutoCase::getIsActive, 1);
                caseCount += Math.toIntExact(autoCaseMapper.selectCount(caseWrapper));
            }
        }
        resp.setAutoSuiteNames(autoSuiteNames);
        resp.setCaseCount(caseCount);

        // 获取最近一次执行记录（COMPLETED 状态）
        LambdaQueryWrapper<TestExecution> execWrapper = new LambdaQueryWrapper<>();
        execWrapper.eq(TestExecution::getPlanId, plan.getId())
                .eq(TestExecution::getStatus, "COMPLETED")
                .orderByDesc(TestExecution::getCreatedAt)
                .last("LIMIT 1");
        List<TestExecution> executions = testExecutionMapper.selectList(execWrapper);
        if (!executions.isEmpty()) {
            TestExecution lastExec = executions.get(0);
            resp.setLastExecutionTime(lastExec.getCreatedAt());
            // 计算通过率
            int total = lastExec.getTotalCases() != null ? lastExec.getTotalCases() : 0;
            int passed = lastExec.getPassedCases() != null ? lastExec.getPassedCases() : 0;
            if (total > 0) {
                resp.setPassRate(Math.round(passed * 1000.0 / total) / 10.0);
            } else {
                resp.setPassRate(0.0);
            }
        }

        return resp;
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseAutoSuiteIds(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String serializeAutoSuiteIds(List<Long> autoSuiteIds) {
        if (autoSuiteIds == null || autoSuiteIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(autoSuiteIds);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "序列化 autoSuiteIds 失败");
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }

    /**
     * 获取分组及所有后代分组 ID（通过 PlanGroupMapper 递归查询）
     */
    private List<Long> getDescendantGroupIds(Long groupId) {
        List<Long> ids = new ArrayList<>();
        ids.add(groupId);

        LambdaQueryWrapper<PlanGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanGroup::getParentId, groupId);
        List<PlanGroup> children = planGroupMapper.selectList(wrapper);
        for (PlanGroup child : children) {
            ids.addAll(getDescendantGroupIds(child.getId()));
        }
        return ids;
    }
}
