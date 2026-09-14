/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.AutoSuiteCreateRequest;
import com.platform.execution.dto.AutoSuitePassRateDTO;
import com.platform.execution.dto.AutoSuiteResponse;
import com.platform.execution.dto.AutoSuiteUpdateRequest;
import com.platform.execution.entity.AutoCase;
import com.platform.execution.entity.AutoSuite;
import com.platform.execution.entity.AutoSuiteGroup;
import com.platform.execution.entity.TestResult;
import com.platform.execution.mapper.AutoCaseMapper;
import com.platform.execution.mapper.AutoSuiteGroupMapper;
import com.platform.execution.mapper.AutoSuiteMapper;
import com.platform.execution.mapper.TestResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 自动化套件服务
 */
@Service
@RequiredArgsConstructor
public class AutoSuiteService {

    private final AutoSuiteMapper autoSuiteMapper;
    private final AutoCaseMapper autoCaseMapper;
    private final TestResultMapper testResultMapper;
    private final AutoSuiteGroupMapper autoSuiteGroupMapper;

    /**
     * 分页查询自动化套件
     *
     * @param groupId  分组 ID（null 表示不过滤；0/-1 表示未分组；正数=指定分组含子孙分组）
     * @param priority 优先级（P0-P3，null 不过滤）
     */
    public PageResponse<AutoSuiteResponse> listAutoSuites(Long projectId, String keyword, Long groupId,
                                                           String priority, int page, int pageSize) {
        LambdaQueryWrapper<AutoSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuite::getProjectId, projectId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AutoSuite::getName, keyword)
                    .or().like(AutoSuite::getDescription, keyword));
        }
        // 分组筛选
        if (groupId != null) {
            if (groupId == 0L || groupId == -1L) {
                // 未分组
                wrapper.isNull(AutoSuite::getGroupId);
            } else {
                // 指定分组（含子孙分组递归）
                wrapper.in(AutoSuite::getGroupId, getDescendantGroupIds(groupId));
            }
        }
        if (StringUtils.hasText(priority)) {
            wrapper.eq(AutoSuite::getPriority, priority);
        }
        wrapper.orderByDesc(AutoSuite::getCreatedAt);

        Page<AutoSuite> result = autoSuiteMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<AutoSuiteResponse> records = new ArrayList<>(result.getRecords().size());
        for (AutoSuite s : result.getRecords()) {
            records.add(toResponse(s, true));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 获取自动化套件详情
     */
    public AutoSuiteResponse getAutoSuite(Long autoSuiteId) {
        AutoSuite suite = autoSuiteMapper.selectById(autoSuiteId);
        if (suite == null) {
            throw new BusinessException(ErrorCode.AUTO_SUITE_NOT_FOUND, "自动化套件不存在：" + autoSuiteId);
        }
        return toResponse(suite, true);
    }

    /**
     * 创建自动化套件
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoSuiteResponse createAutoSuite(Long projectId, AutoSuiteCreateRequest request) {
        // 名称唯一性校验
        if (countByName(projectId, request.getName(), null) > 0) {
            throw new BusinessException(ErrorCode.AUTO_SUITE_NAME_DUPLICATE, "自动化套件名称已存在：" + request.getName());
        }

        AutoSuite suite = new AutoSuite();
        BeanUtils.copyProperties(request, suite);
        suite.setProjectId(projectId);
        if (!StringUtils.hasText(suite.getPriority())) {
            suite.setPriority("P2");
        }
        if (suite.getEnableOnceSetupTeardown() == null) {
            suite.setEnableOnceSetupTeardown(0);
        }
        if (suite.getEnablePerCaseSetupTeardown() == null) {
            suite.setEnablePerCaseSetupTeardown(0);
        }
        suite.setCreatedBy(getCurrentUserId());
        autoSuiteMapper.insert(suite);
        return toResponse(suite, false);
    }

    /**
     * 更新自动化套件
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoSuiteResponse updateAutoSuite(Long autoSuiteId, AutoSuiteUpdateRequest request) {
        AutoSuite suite = autoSuiteMapper.selectById(autoSuiteId);
        if (suite == null) {
            throw new BusinessException(ErrorCode.AUTO_SUITE_NOT_FOUND, "自动化套件不存在：" + autoSuiteId);
        }
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(suite.getName())) {
            if (countByName(suite.getProjectId(), request.getName(), autoSuiteId) > 0) {
                throw new BusinessException(ErrorCode.AUTO_SUITE_NAME_DUPLICATE, "自动化套件名称已存在：" + request.getName());
            }
            suite.setName(request.getName());
        }
        if (request.getDescription() != null) {
            suite.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            suite.setPriority(request.getPriority());
        }
        if (request.getGroupId() != null) {
            suite.setGroupId(request.getGroupId());
        }
        if (request.getTags() != null) {
            suite.setTags(request.getTags());
        }
        if (request.getEnableOnceSetupTeardown() != null) {
            suite.setEnableOnceSetupTeardown(request.getEnableOnceSetupTeardown());
        }
        if (request.getOnceSetupSteps() != null) {
            suite.setOnceSetupSteps(request.getOnceSetupSteps());
        }
        if (request.getOnceTeardownSteps() != null) {
            suite.setOnceTeardownSteps(request.getOnceTeardownSteps());
        }
        if (request.getEnablePerCaseSetupTeardown() != null) {
            suite.setEnablePerCaseSetupTeardown(request.getEnablePerCaseSetupTeardown());
        }
        if (request.getPerCaseSetupSteps() != null) {
            suite.setPerCaseSetupSteps(request.getPerCaseSetupSteps());
        }
        if (request.getPerCaseTeardownSteps() != null) {
            suite.setPerCaseTeardownSteps(request.getPerCaseTeardownSteps());
        }
        autoSuiteMapper.updateById(suite);
        return toResponse(suite, true);
    }

    /**
     * 删除自动化套件（自动化用例由外键 ON DELETE CASCADE 级联删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAutoSuite(Long autoSuiteId) {
        AutoSuite suite = autoSuiteMapper.selectById(autoSuiteId);
        if (suite == null) {
            throw new BusinessException(ErrorCode.AUTO_SUITE_NOT_FOUND, "自动化套件不存在：" + autoSuiteId);
        }
        autoSuiteMapper.deleteById(autoSuiteId);
    }

    /**
     * 清空分组及其子孙分组中的所有自动化套件（自动化用例与执行结果由外键级联删除）
     *
     * @param groupId 分组 ID（0 表示未分组）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByGroup(Long projectId, Long groupId) {
        LambdaQueryWrapper<AutoSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuite::getProjectId, projectId);
        if (groupId == 0L) {
            // 未分组
            wrapper.isNull(AutoSuite::getGroupId);
        } else {
            // 指定分组（含子孙分组递归）
            wrapper.in(AutoSuite::getGroupId, getDescendantGroupIds(groupId));
        }
        autoSuiteMapper.delete(wrapper);
    }

    /**
     * 清空项目下所有自动化套件
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByProject(Long projectId) {
        LambdaQueryWrapper<AutoSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuite::getProjectId, projectId);
        autoSuiteMapper.delete(wrapper);
    }

    /**
     * 批量计算自动化套件通过率
     *
     * <p>对每个自动化套件：查询其下所有自动化用例，取每条自动化用例最近一次执行结果，
     * 计算 PASSED / 总数 作为通过率。
     */
    public List<AutoSuitePassRateDTO> getPassRates(Long projectId, List<Long> autoSuiteIds) {
        if (autoSuiteIds == null || autoSuiteIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<AutoSuitePassRateDTO> result = new ArrayList<>(autoSuiteIds.size());
        for (Long autoSuiteId : autoSuiteIds) {
            AutoSuitePassRateDTO dto = new AutoSuitePassRateDTO();
            dto.setAutoSuiteId(autoSuiteId);

            // 查询自动化套件下所有自动化用例 ID
            LambdaQueryWrapper<AutoCase> caseWrapper = new LambdaQueryWrapper<>();
            caseWrapper.eq(AutoCase::getAutoSuiteId, autoSuiteId).select(AutoCase::getId);
            List<AutoCase> cases = autoCaseMapper.selectList(caseWrapper);

            if (cases.isEmpty()) {
                dto.setPassRate(-1);
                dto.setTotalCases(0);
                result.add(dto);
                continue;
            }

            Set<Long> caseIds = new HashSet<>();
            for (AutoCase c : cases) {
                caseIds.add(c.getId());
            }

            // 查询这些自动化用例的所有执行结果，按 autoCaseId + finishedAt 降序
            LambdaQueryWrapper<TestResult> resultWrapper = new LambdaQueryWrapper<>();
            resultWrapper.in(TestResult::getAutoCaseId, caseIds);
            resultWrapper.isNotNull(TestResult::getFinishedAt);
            resultWrapper.orderByDesc(TestResult::getFinishedAt);
            List<TestResult> results = testResultMapper.selectList(resultWrapper);

            // 取每个自动化用例的最新结果
            Map<Long, String> latestStatusMap = new LinkedHashMap<>();
            for (TestResult tr : results) {
                if (!latestStatusMap.containsKey(tr.getAutoCaseId())) {
                    latestStatusMap.put(tr.getAutoCaseId(), tr.getStatus());
                }
            }

            if (latestStatusMap.isEmpty()) {
                dto.setPassRate(-1);
                dto.setTotalCases(cases.size());
            } else {
                int passed = 0;
                for (String status : latestStatusMap.values()) {
                    if ("PASSED".equals(status)) {
                        passed++;
                    }
                }
                int total = latestStatusMap.size();
                dto.setPassRate(total > 0 ? (passed * 100 / total) : -1);
                dto.setTotalCases(total);
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * 批量修改自动化套件分组
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateGroup(List<Long> autoSuiteIds, Long groupId) {
        if (autoSuiteIds == null || autoSuiteIds.isEmpty()) {
            return;
        }
        for (Long autoSuiteId : autoSuiteIds) {
            AutoSuite suite = autoSuiteMapper.selectById(autoSuiteId);
            if (suite != null) {
                suite.setGroupId(groupId);
                autoSuiteMapper.updateById(suite);
            }
        }
    }

    /**
     * 获取分组及所有后代分组 ID（递归查询）
     */
    private List<Long> getDescendantGroupIds(Long groupId) {
        List<Long> ids = new ArrayList<>();
        ids.add(groupId);

        LambdaQueryWrapper<AutoSuiteGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuiteGroup::getParentId, groupId);
        List<AutoSuiteGroup> children = autoSuiteGroupMapper.selectList(wrapper);
        for (AutoSuiteGroup child : children) {
            ids.addAll(getDescendantGroupIds(child.getId()));
        }
        return ids;
    }

    private long countByName(Long projectId, String name, Long excludeId) {
        LambdaQueryWrapper<AutoSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuite::getProjectId, projectId);
        wrapper.eq(AutoSuite::getName, name);
        if (excludeId != null) {
            wrapper.ne(AutoSuite::getId, excludeId);
        }
        return autoSuiteMapper.selectCount(wrapper);
    }

    private AutoSuiteResponse toResponse(AutoSuite suite, boolean withCaseCount) {
        AutoSuiteResponse r = new AutoSuiteResponse();
        BeanUtils.copyProperties(suite, r);
        if (withCaseCount) {
            LambdaQueryWrapper<AutoCase> w = new LambdaQueryWrapper<>();
            w.eq(AutoCase::getAutoSuiteId, suite.getId());
            r.setCaseCount(autoCaseMapper.selectCount(w));
        }
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
