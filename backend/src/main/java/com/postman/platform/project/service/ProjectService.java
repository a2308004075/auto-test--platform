package com.postman.platform.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.postman.platform.apidoc.entity.Api;
import com.postman.platform.apidoc.mapper.ApiMapper;
import com.postman.platform.common.exception.BusinessException;
import com.postman.platform.common.exception.ErrorCode;
import com.postman.platform.common.response.PageResponse;
import com.postman.platform.execution.entity.TestExecution;
import com.postman.platform.execution.entity.TestCase;
import com.postman.platform.execution.entity.TestPlan;
import com.postman.platform.execution.entity.TestSuite;
import com.postman.platform.execution.mapper.TestExecutionMapper;
import com.postman.platform.execution.mapper.TestPlanMapper;
import com.postman.platform.execution.mapper.TestCaseMapper;
import com.postman.platform.execution.mapper.TestSuiteMapper;
import com.postman.platform.keyword.entity.Keyword;
import com.postman.platform.keyword.mapper.KeywordMapper;
import com.postman.platform.project.dto.*;
import com.postman.platform.project.entity.ApiModule;
import com.postman.platform.project.entity.Project;
import com.postman.platform.project.mapper.ApiModuleMapper;
import com.postman.platform.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目管理服务
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ApiModuleMapper apiModuleMapper;
    private final ApiMapper apiMapper;
    private final KeywordMapper keywordMapper;
    private final TestSuiteMapper testSuiteMapper;
    private final TestCaseMapper testCaseMapper;
    private final TestPlanMapper testPlanMapper;
    private final TestExecutionMapper testExecutionMapper;

    /**
     * 分页查询项目列表
     */
    public PageResponse<ProjectResponse> listProjects(String keyword, int page, int pageSize) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Project::getName, keyword)
                    .or().like(Project::getDescription, keyword));
        }
        wrapper.orderByDesc(Project::getCreatedAt);

        Page<Project> pageParam = new Page<>(page, pageSize);
        Page<Project> result = projectMapper.selectPage(pageParam, wrapper);

        List<ProjectResponse> records = new ArrayList<>();
        for (Project p : result.getRecords()) {
            records.add(toResponse(p));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 创建项目（含自动创建系统分组）
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectResponse createProject(ProjectCreateRequest request) {
        // 名称唯一性检查
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getName, request.getName());
        if (projectMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PROJECT_NAME_DUPLICATE, "项目名称已存在：" + request.getName());
        }

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setSourcePath(request.getSourcePath());
        project.setIsActive(true);
        projectMapper.insert(project);

        // 自动创建系统分组：「全部」和「未分组」
        createSystemModule(project.getId(), "全部", null, "系统默认分组，包含所有接口");
        createSystemModule(project.getId(), "未分组", null, "未分组的接口");

        return toResponse(project);
    }

    /**
     * 获取项目详情
     */
    public ProjectResponse getProject(String projectId) {
        return toResponse(findActiveById(projectId));
    }

    /**
     * 更新项目
     */
    public ProjectResponse updateProject(String projectId, ProjectUpdateRequest request) {
        Project project = findActiveById(projectId);

        // 名称唯一性检查（排除自身）
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(project.getName())) {
            LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Project::getName, request.getName());
            wrapper.ne(Project::getId, projectId);
            if (projectMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.PROJECT_NAME_DUPLICATE, "项目名称已存在：" + request.getName());
            }
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getSourcePath() != null) {
            project.setSourcePath(request.getSourcePath());
        }

        projectMapper.updateById(project);
        return toResponse(project);
    }

    /**
     * 删除项目（软删除）
     */
    public void deleteProject(String projectId) {
        findActiveById(projectId);
        projectMapper.deleteById(projectId);
    }

    /**
     * 启停项目
     */
    public ProjectResponse toggleStatus(String projectId) {
        Project project = findActiveById(projectId);
        project.setIsActive(!project.getIsActive());
        projectMapper.updateById(project);
        return toResponse(project);
    }

    /**
     * 获取项目仪表板
     */
    public ProjectDashboardResponse getDashboard(String projectId) {
        Project project = findActiveById(projectId);

        DashboardStats stats = new DashboardStats();

        // 接口数
        LambdaQueryWrapper<Api> apiWrapper = new LambdaQueryWrapper<>();
        apiWrapper.eq(Api::getProjectId, projectId);
        stats.setApiCount(apiMapper.selectCount(apiWrapper));

        // 关键字数
        LambdaQueryWrapper<Keyword> kwWrapper = new LambdaQueryWrapper<>();
        kwWrapper.eq(Keyword::getProjectId, projectId);
        stats.setKeywordCount(keywordMapper.selectCount(kwWrapper));

        // 套件数
        stats.setSuiteCount((long) getSuiteIds(projectId).size());

        // 用例数
        List<String> suiteIds = getSuiteIds(projectId);
        if (!suiteIds.isEmpty()) {
            LambdaQueryWrapper<TestCase> caseWrapper = new LambdaQueryWrapper<>();
            caseWrapper.in(TestCase::getSuiteId, suiteIds);
            stats.setCaseCount(testCaseMapper.selectCount(caseWrapper));
        }

        // 计划数
        stats.setPlanCount((long) getPlanIds(projectId).size());

        // 执行统计
        List<String> planIds = getPlanIds(projectId);
        if (!planIds.isEmpty()) {
            LambdaQueryWrapper<TestExecution> execWrapper = new LambdaQueryWrapper<>();
            execWrapper.in(TestExecution::getPlanId, planIds);
            List<TestExecution> executions = testExecutionMapper.selectList(execWrapper);
            stats.setExecutionCount((long) executions.size());

            long passed = 0, failed = 0;
            for (TestExecution e : executions) {
                if (e.getPassedCases() != null) passed += e.getPassedCases();
                if (e.getFailedCases() != null) failed += e.getFailedCases();
            }
            stats.setPassedCases(passed);
            stats.setFailedCases(failed);
            long total = passed + failed;
            stats.setPassRate(total > 0 ? Math.round(passed * 1000.0 / total) / 10.0 : 0.0);
        }

        // 最近执行记录
        List<RecentExecution> recentExecutions = new ArrayList<>();
        if (!planIds.isEmpty()) {
            LambdaQueryWrapper<TestExecution> recentWrapper = new LambdaQueryWrapper<>();
            recentWrapper.in(TestExecution::getPlanId, planIds)
                    .orderByDesc(TestExecution::getCreatedAt)
                    .last("LIMIT 5");
            List<TestExecution> recent = testExecutionMapper.selectList(recentWrapper);
            for (TestExecution e : recent) {
                RecentExecution re = new RecentExecution();
                BeanUtils.copyProperties(e, re);
                TestPlan plan = testPlanMapper.selectById(e.getPlanId());
                if (plan != null) {
                    re.setPlanName(plan.getName());
                }
                recentExecutions.add(re);
            }
        }

        ProjectDashboardResponse response = new ProjectDashboardResponse();
        response.setProjectId(project.getId());
        response.setProjectName(project.getName());
        response.setStats(stats);
        response.setRecentExecutions(recentExecutions);
        return response;
    }

    /**
     * 根据 ID 查找有效项目
     */
    public Project findActiveById(String projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getIsActive()) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在：" + projectId);
        }
        return project;
    }

    // ───────────────────── 私有方法 ─────────────────────

    private List<String> getSuiteIds(String projectId) {
        LambdaQueryWrapper<TestSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestSuite::getProjectId, projectId)
                .select(TestSuite::getId);
        return testSuiteMapper.selectList(wrapper).stream()
                .map(TestSuite::getId).collect(Collectors.toList());
    }

    private List<String> getPlanIds(String projectId) {
        LambdaQueryWrapper<TestPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestPlan::getProjectId, projectId)
                .select(TestPlan::getId);
        return testPlanMapper.selectList(wrapper).stream()
                .map(TestPlan::getId).collect(Collectors.toList());
    }

    private void createSystemModule(String projectId, String name, String prefix, String description) {
        ApiModule module = new ApiModule();
        module.setProjectId(projectId);
        module.setName(name);
        module.setServicePrefix(prefix);
        module.setDescription(description);
        module.setSourceType("MANUAL");
        module.setIsSystem(true);
        apiModuleMapper.insert(module);
    }

    private ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        BeanUtils.copyProperties(project, response);
        return response;
    }
}
