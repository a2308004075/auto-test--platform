/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试用例管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.CaseCreateRequest;
import com.platform.execution.dto.CaseDebugRequest;
import com.platform.execution.dto.CaseDebugResponse;
import com.platform.execution.dto.CaseResponse;
import com.platform.execution.dto.CaseUpdateRequest;
import com.platform.execution.engine.CaseExecutor;
import com.platform.execution.engine.ExecutionContext;
import com.platform.execution.engine.StepResult;
import com.platform.execution.entity.DefectRelation;
import com.platform.execution.entity.TestCase;
import com.platform.execution.entity.TestSuite;
import com.platform.execution.mapper.DefectRelationMapper;
import com.platform.execution.mapper.TestCaseMapper;
import com.platform.execution.mapper.TestSuiteMapper;
import com.platform.environment.service.EnvironmentService;
import com.platform.requirement.service.RequirementCaseRelationService;
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
import java.util.Map;
import java.util.Set;

/**
 * 测试用例服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final TestCaseMapper testCaseMapper;
    private final TestSuiteMapper testSuiteMapper;
    private final CaseGroupService caseGroupService;
    private final CaseExecutor caseExecutor;
    private final EnvironmentService environmentService;
    private final RequirementCaseRelationService requirementCaseRelationService;
    private final DefectRelationMapper defectRelationMapper;

    /**
     * 分页查询测试用例
     *
     * @param projectId 项目 ID（用例无项目字段，经所属套件限定项目范围）
     * @param groupId   分组 ID（null=不过滤，0=未分组，正数=指定分组含子孙分组）
     */
    public PageResponse<CaseResponse> listCases(Long projectId, Long suiteId, Long groupId, String keyword,
                                                 String priority, String status, int page, int pageSize) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        // 用例无项目字段，经所属套件限定项目范围
        if (projectId != null) {
            List<Long> suiteIds = getProjectSuiteIds(projectId);
            if (suiteIds.isEmpty()) {
                return PageResponse.empty((long) page, (long) pageSize);
            }
            wrapper.in(TestCase::getSuiteId, suiteIds);
        }
        if (suiteId != null) {
            wrapper.eq(TestCase::getSuiteId, suiteId);
        }
        // 按分组筛选（0=未分组；正数=含子孙分组）
        if (groupId != null) {
            if (groupId == 0L) {
                // 未分组
                wrapper.isNull(TestCase::getGroupId);
            } else {
                Set<Long> groupIds = caseGroupService.getDescendantGroupIds(groupId);
                wrapper.in(TestCase::getGroupId, groupIds);
            }
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TestCase::getName, keyword)
                    .or().like(TestCase::getDescription, keyword));
        }
        if (StringUtils.hasText(priority)) {
            wrapper.eq(TestCase::getPriority, priority);
        }
        if (StringUtils.hasText(status)) {
            if ("1".equals(status)) {
                wrapper.eq(TestCase::getIsActive, 1);
            } else if ("0".equals(status)) {
                wrapper.eq(TestCase::getIsActive, 0);
            }
        }
        wrapper.orderByDesc(TestCase::getCreatedAt);

        Page<TestCase> result = testCaseMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<CaseResponse> records = new ArrayList<>(result.getRecords().size());
        for (TestCase c : result.getRecords()) {
            records.add(toResponse(c));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 获取用例详情
     */
    public CaseResponse getCase(Long caseId) {
        TestCase c = testCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND, "测试用例不存在：" + caseId);
        }
        return toResponse(c);
    }

    /**
     * 创建测试用例
     */
    @Transactional(rollbackFor = Exception.class)
    public CaseResponse createCase(Long suiteId, CaseCreateRequest request) {
        TestSuite suite = testSuiteMapper.selectById(suiteId);
        if (suite == null) {
            throw new BusinessException(ErrorCode.SUITE_NOT_FOUND, "测试套件不存在：" + suiteId);
        }
        if (countByName(suiteId, request.getName(), null) > 0) {
            throw new BusinessException(ErrorCode.CASE_NAME_DUPLICATE, "用例名称已存在：" + request.getName());
        }

        TestCase c = new TestCase();
        BeanUtils.copyProperties(request, c);
        c.setSuiteId(suiteId);
        if (!StringUtils.hasText(c.getPriority())) {
            c.setPriority("中");
        }
        if (c.getTimeout() == null) {
            c.setTimeout(30);
        }
        if (c.getSteps() == null) {
            c.setSteps("[]");
        }
        c.setIsActive(1);
        c.setCreatedBy(getCurrentUserId());
        testCaseMapper.insert(c);
        return toResponse(c);
    }

    /**
     * 更新测试用例
     */
    @Transactional(rollbackFor = Exception.class)
    public CaseResponse updateCase(Long caseId, CaseUpdateRequest request) {
        TestCase c = testCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND, "测试用例不存在：" + caseId);
        }
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(c.getName())) {
            if (countByName(c.getSuiteId(), request.getName(), caseId) > 0) {
                throw new BusinessException(ErrorCode.CASE_NAME_DUPLICATE, "用例名称已存在：" + request.getName());
            }
            c.setName(request.getName());
        }
        if (request.getDescription() != null) {
            c.setDescription(request.getDescription());
        }
        if (request.getPreconditions() != null) {
            c.setPreconditions(request.getPreconditions());
        }
        if (request.getSetupSteps() != null) {
            c.setSetupSteps(request.getSetupSteps());
        }
        if (request.getTeardownSteps() != null) {
            c.setTeardownSteps(request.getTeardownSteps());
        }
        if (request.getSteps() != null) {
            c.setSteps(request.getSteps());
        }
        if (request.getPriority() != null) {
            c.setPriority(request.getPriority());
        }
        if (request.getTimeout() != null) {
            c.setTimeout(request.getTimeout());
        }
        if (request.getIsActive() != null) {
            c.setIsActive(request.getIsActive());
        }
        if (request.getGroupId() != null) {
            c.setGroupId(request.getGroupId());
        }
        if (request.getTags() != null) {
            c.setTags(request.getTags());
        }
        testCaseMapper.updateById(c);
        return toResponse(c);
    }

    /**
     * 删除测试用例（同步清理需求关联与缺陷关联）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long caseId) {
        TestCase c = testCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND, "测试用例不存在：" + caseId);
        }
        deleteCaseRelations(caseId);
        testCaseMapper.deleteById(caseId);
    }

    /**
     * 清空分组及其子孙分组中的所有用例（同步清理需求关联与缺陷关联）
     *
     * @param groupId 分组 ID（0 表示未分组）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByGroup(Long projectId, Long groupId) {
        List<Long> suiteIds = getProjectSuiteIds(projectId);
        if (suiteIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        // 用例无项目字段，经所属套件限定项目范围
        wrapper.in(TestCase::getSuiteId, suiteIds);
        if (groupId == 0L) {
            // 未分组
            wrapper.isNull(TestCase::getGroupId);
        } else {
            // 指定分组（含子孙分组递归）
            wrapper.in(TestCase::getGroupId, caseGroupService.getDescendantGroupIds(groupId));
        }
        List<TestCase> cases = testCaseMapper.selectList(wrapper);
        for (TestCase c : cases) {
            deleteCaseRelations(c.getId());
        }
        testCaseMapper.delete(wrapper);
    }

    /**
     * 清空项目下所有用例（同步清理需求关联与缺陷关联）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByProject(Long projectId) {
        List<Long> suiteIds = getProjectSuiteIds(projectId);
        if (suiteIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TestCase::getSuiteId, suiteIds);
        List<TestCase> cases = testCaseMapper.selectList(wrapper);
        for (TestCase c : cases) {
            deleteCaseRelations(c.getId());
        }
        testCaseMapper.delete(wrapper);
    }

    /**
     * 删除某自动用例的需求关联与缺陷关联记录
     */
    private void deleteCaseRelations(Long caseId) {
        requirementCaseRelationService.deleteByCase(RequirementCaseRelationService.CASE_TYPE_AUTO, caseId);

        LambdaQueryWrapper<DefectRelation> defectWrapper = new LambdaQueryWrapper<>();
        defectWrapper.eq(DefectRelation::getTargetType, "TEST_CASE")
                .eq(DefectRelation::getTargetId, caseId);
        defectRelationMapper.delete(defectWrapper);
    }

    /**
     * 启用/禁用测试用例
     */
    @Transactional(rollbackFor = Exception.class)
    public CaseResponse toggleStatus(Long caseId) {
        TestCase c = testCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND, "测试用例不存在：" + caseId);
        }
        c.setIsActive(Integer.valueOf(1).equals(c.getIsActive()) ? 0 : 1);
        testCaseMapper.updateById(c);
        return toResponse(c);
    }

    /**
     * 用例调试：同步执行单条用例并返回结果
     */
    public CaseDebugResponse debugCase(Long caseId, CaseDebugRequest request) {
        TestCase testCase = testCaseMapper.selectById(caseId);
        if (testCase == null) {
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND, "测试用例不存在：" + caseId);
        }

        // 从套件获取项目 ID
        TestSuite suite = testSuiteMapper.selectById(testCase.getSuiteId());
        Long projectId = suite != null ? suite.getProjectId() : null;

        // 构建执行上下文
        ExecutionContext context = new ExecutionContext();
        context.setProjectId(projectId);

        // 加载环境变量（如果指定了环境）
        if (request != null && request.getEnvironmentId() != null) {
            context.setEnvironmentId(request.getEnvironmentId());
            try {
                Map<String, String> variables = environmentService.getVariablesAsMap(request.getEnvironmentId());
                String host = variables.get("host");
                if (host != null && !host.isEmpty()) {
                    context.setBaseUrl(host);
                }
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                log.warn("加载环境变量失败: {}", e.getMessage());
            }
        }

        // 同步执行用例
        StepResult result = caseExecutor.execute(testCase, context);

        // 构建响应
        CaseDebugResponse response = new CaseDebugResponse();
        response.setStatus(result.getStatus());
        response.setMessage(result.getMessage());
        response.setDurationMs(result.getDurationMs());

        if (result.getResponse() != null && result.getResponse().containsKey("stepLogs")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stepLogs = (List<Map<String, Object>>) result.getResponse().get("stepLogs");
            response.setStepLogs(stepLogs);
        } else {
            response.setStepLogs(Collections.emptyList());
        }

        return response;
    }

    private long countByName(Long suiteId, String name, Long excludeId) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestCase::getSuiteId, suiteId);
        wrapper.eq(TestCase::getName, name);
        if (excludeId != null) {
            wrapper.ne(TestCase::getId, excludeId);
        }
        return testCaseMapper.selectCount(wrapper);
    }

    /**
     * 查询项目下所有套件 ID（用例无项目字段，经套件关联项目）
     */
    private List<Long> getProjectSuiteIds(Long projectId) {
        LambdaQueryWrapper<TestSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestSuite::getProjectId, projectId).select(TestSuite::getId);
        List<Long> ids = new ArrayList<>();
        for (TestSuite s : testSuiteMapper.selectList(wrapper)) {
            ids.add(s.getId());
        }
        return ids;
    }

    private CaseResponse toResponse(TestCase c) {
        CaseResponse r = new CaseResponse();
        BeanUtils.copyProperties(c, r);
        return r;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }
}
