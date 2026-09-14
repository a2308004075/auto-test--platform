/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化用例管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.AutoCaseCreateRequest;
import com.platform.execution.dto.AutoCaseDebugRequest;
import com.platform.execution.dto.AutoCaseDebugResponse;
import com.platform.execution.dto.AutoCaseResponse;
import com.platform.execution.dto.AutoCaseUpdateRequest;
import com.platform.execution.engine.AutoCaseExecutor;
import com.platform.execution.engine.ExecutionContext;
import com.platform.execution.engine.StepResult;
import com.platform.execution.entity.AutoCase;
import com.platform.execution.entity.AutoSuite;
import com.platform.execution.entity.DefectRelation;
import com.platform.execution.mapper.AutoCaseMapper;
import com.platform.execution.mapper.AutoSuiteMapper;
import com.platform.execution.mapper.DefectRelationMapper;
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
 * 自动化用例服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCaseService {

    private final AutoCaseMapper autoCaseMapper;
    private final AutoSuiteMapper autoSuiteMapper;
    private final AutoCaseGroupService autoCaseGroupService;
    private final AutoCaseExecutor autoCaseExecutor;
    private final EnvironmentService environmentService;
    private final RequirementCaseRelationService requirementCaseRelationService;
    private final DefectRelationMapper defectRelationMapper;

    /**
     * 分页查询自动化用例
     *
     * @param projectId 项目 ID（自动化用例无项目字段，经所属自动化套件限定项目范围）
     * @param groupId   分组 ID（null=不过滤，0=未分组，正数=指定分组含子孙分组）
     */
    public PageResponse<AutoCaseResponse> listAutoCases(Long projectId, Long autoSuiteId, Long groupId, String keyword,
                                                         String priority, String status, int page, int pageSize) {
        LambdaQueryWrapper<AutoCase> wrapper = new LambdaQueryWrapper<>();
        // 自动化用例无项目字段，经所属自动化套件限定项目范围
        if (projectId != null) {
            List<Long> autoSuiteIds = getProjectAutoSuiteIds(projectId);
            if (autoSuiteIds.isEmpty()) {
                return PageResponse.empty((long) page, (long) pageSize);
            }
            wrapper.in(AutoCase::getAutoSuiteId, autoSuiteIds);
        }
        if (autoSuiteId != null) {
            wrapper.eq(AutoCase::getAutoSuiteId, autoSuiteId);
        }
        // 按分组筛选（0=未分组；正数=含子孙分组）
        if (groupId != null) {
            if (groupId == 0L) {
                // 未分组
                wrapper.isNull(AutoCase::getGroupId);
            } else {
                Set<Long> groupIds = autoCaseGroupService.getDescendantGroupIds(groupId);
                wrapper.in(AutoCase::getGroupId, groupIds);
            }
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AutoCase::getName, keyword)
                    .or().like(AutoCase::getDescription, keyword));
        }
        if (StringUtils.hasText(priority)) {
            wrapper.eq(AutoCase::getPriority, priority);
        }
        if (StringUtils.hasText(status)) {
            if ("1".equals(status)) {
                wrapper.eq(AutoCase::getIsActive, 1);
            } else if ("0".equals(status)) {
                wrapper.eq(AutoCase::getIsActive, 0);
            }
        }
        wrapper.orderByDesc(AutoCase::getCreatedAt);

        Page<AutoCase> result = autoCaseMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<AutoCaseResponse> records = new ArrayList<>(result.getRecords().size());
        for (AutoCase c : result.getRecords()) {
            records.add(toResponse(c));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 获取自动化用例详情
     */
    public AutoCaseResponse getAutoCase(Long autoCaseId) {
        AutoCase c = autoCaseMapper.selectById(autoCaseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.AUTO_CASE_NOT_FOUND, "自动化用例不存在：" + autoCaseId);
        }
        return toResponse(c);
    }

    /**
     * 创建自动化用例
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoCaseResponse createAutoCase(Long autoSuiteId, AutoCaseCreateRequest request) {
        AutoSuite suite = autoSuiteMapper.selectById(autoSuiteId);
        if (suite == null) {
            throw new BusinessException(ErrorCode.AUTO_SUITE_NOT_FOUND, "自动化套件不存在：" + autoSuiteId);
        }
        if (countByName(autoSuiteId, request.getName(), null) > 0) {
            throw new BusinessException(ErrorCode.AUTO_CASE_NAME_DUPLICATE, "自动化用例名称已存在：" + request.getName());
        }

        AutoCase c = new AutoCase();
        BeanUtils.copyProperties(request, c);
        c.setAutoSuiteId(autoSuiteId);
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
        autoCaseMapper.insert(c);
        return toResponse(c);
    }

    /**
     * 更新自动化用例
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoCaseResponse updateAutoCase(Long autoCaseId, AutoCaseUpdateRequest request) {
        AutoCase c = autoCaseMapper.selectById(autoCaseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.AUTO_CASE_NOT_FOUND, "自动化用例不存在：" + autoCaseId);
        }
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(c.getName())) {
            if (countByName(c.getAutoSuiteId(), request.getName(), autoCaseId) > 0) {
                throw new BusinessException(ErrorCode.AUTO_CASE_NAME_DUPLICATE, "自动化用例名称已存在：" + request.getName());
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
        autoCaseMapper.updateById(c);
        return toResponse(c);
    }

    /**
     * 删除自动化用例（同步清理需求关联与缺陷关联）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAutoCase(Long autoCaseId) {
        AutoCase c = autoCaseMapper.selectById(autoCaseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.AUTO_CASE_NOT_FOUND, "自动化用例不存在：" + autoCaseId);
        }
        deleteCaseRelations(autoCaseId);
        autoCaseMapper.deleteById(autoCaseId);
    }

    /**
     * 清空分组及其子孙分组中的所有自动化用例（同步清理需求关联与缺陷关联）
     *
     * @param groupId 分组 ID（0 表示未分组）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByGroup(Long projectId, Long groupId) {
        List<Long> autoSuiteIds = getProjectAutoSuiteIds(projectId);
        if (autoSuiteIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<AutoCase> wrapper = new LambdaQueryWrapper<>();
        // 自动化用例无项目字段，经所属自动化套件限定项目范围
        wrapper.in(AutoCase::getAutoSuiteId, autoSuiteIds);
        if (groupId == 0L) {
            // 未分组
            wrapper.isNull(AutoCase::getGroupId);
        } else {
            // 指定分组（含子孙分组递归）
            wrapper.in(AutoCase::getGroupId, autoCaseGroupService.getDescendantGroupIds(groupId));
        }
        List<AutoCase> cases = autoCaseMapper.selectList(wrapper);
        for (AutoCase c : cases) {
            deleteCaseRelations(c.getId());
        }
        autoCaseMapper.delete(wrapper);
    }

    /**
     * 清空项目下所有自动化用例（同步清理需求关联与缺陷关联）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByProject(Long projectId) {
        List<Long> autoSuiteIds = getProjectAutoSuiteIds(projectId);
        if (autoSuiteIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<AutoCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AutoCase::getAutoSuiteId, autoSuiteIds);
        List<AutoCase> cases = autoCaseMapper.selectList(wrapper);
        for (AutoCase c : cases) {
            deleteCaseRelations(c.getId());
        }
        autoCaseMapper.delete(wrapper);
    }

    /**
     * 删除某自动化用例的需求关联与缺陷关联记录
     */
    private void deleteCaseRelations(Long autoCaseId) {
        requirementCaseRelationService.deleteByCase(RequirementCaseRelationService.CASE_TYPE_AUTO, autoCaseId);

        LambdaQueryWrapper<DefectRelation> defectWrapper = new LambdaQueryWrapper<>();
        defectWrapper.eq(DefectRelation::getTargetType, "AUTO_CASE")
                .eq(DefectRelation::getTargetId, autoCaseId);
        defectRelationMapper.delete(defectWrapper);
    }

    /**
     * 启用/禁用自动化用例
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoCaseResponse toggleStatus(Long autoCaseId) {
        AutoCase c = autoCaseMapper.selectById(autoCaseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.AUTO_CASE_NOT_FOUND, "自动化用例不存在：" + autoCaseId);
        }
        c.setIsActive(Integer.valueOf(1).equals(c.getIsActive()) ? 0 : 1);
        autoCaseMapper.updateById(c);
        return toResponse(c);
    }

    /**
     * 自动化用例调试：同步执行单条自动化用例并返回结果
     */
    public AutoCaseDebugResponse debugAutoCase(Long autoCaseId, AutoCaseDebugRequest request) {
        AutoCase autoCase = autoCaseMapper.selectById(autoCaseId);
        if (autoCase == null) {
            throw new BusinessException(ErrorCode.AUTO_CASE_NOT_FOUND, "自动化用例不存在：" + autoCaseId);
        }

        // 从自动化套件获取项目 ID
        AutoSuite suite = autoSuiteMapper.selectById(autoCase.getAutoSuiteId());
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

        // 同步执行自动化用例
        StepResult result = autoCaseExecutor.execute(autoCase, context);

        // 构建响应
        AutoCaseDebugResponse response = new AutoCaseDebugResponse();
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

    private long countByName(Long autoSuiteId, String name, Long excludeId) {
        LambdaQueryWrapper<AutoCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoCase::getAutoSuiteId, autoSuiteId);
        wrapper.eq(AutoCase::getName, name);
        if (excludeId != null) {
            wrapper.ne(AutoCase::getId, excludeId);
        }
        return autoCaseMapper.selectCount(wrapper);
    }

    /**
     * 查询项目下所有自动化套件 ID（自动化用例无项目字段，经自动化套件关联项目）
     */
    private List<Long> getProjectAutoSuiteIds(Long projectId) {
        LambdaQueryWrapper<AutoSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuite::getProjectId, projectId).select(AutoSuite::getId);
        List<Long> ids = new ArrayList<>();
        for (AutoSuite s : autoSuiteMapper.selectList(wrapper)) {
            ids.add(s.getId());
        }
        return ids;
    }

    private AutoCaseResponse toResponse(AutoCase c) {
        AutoCaseResponse r = new AutoCaseResponse();
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
