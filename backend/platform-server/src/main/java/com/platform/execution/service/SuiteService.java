/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试套件管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.SuiteCreateRequest;
import com.platform.execution.dto.SuitePassRateDTO;
import com.platform.execution.dto.SuiteResponse;
import com.platform.execution.dto.SuiteUpdateRequest;
import com.platform.execution.entity.TestCase;
import com.platform.execution.entity.TestResult;
import com.platform.execution.entity.TestSuite;
import com.platform.execution.mapper.TestCaseMapper;
import com.platform.execution.mapper.TestResultMapper;
import com.platform.execution.mapper.TestSuiteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 测试套件服务
 */
@Service
@RequiredArgsConstructor
public class SuiteService {

    private final TestSuiteMapper testSuiteMapper;
    private final TestCaseMapper testCaseMapper;
    private final TestResultMapper testResultMapper;

    /**
     * 分页查询测试套件
     *
     * @param groupId 分组 ID（null 表示不过滤；-1 表示未分组）
     */
    public PageResponse<SuiteResponse> listSuites(Long projectId, String keyword, Long groupId, int page, int pageSize) {
        LambdaQueryWrapper<TestSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestSuite::getProjectId, projectId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TestSuite::getName, keyword)
                    .or().like(TestSuite::getDescription, keyword));
        }
        // 分组筛选
        if (groupId != null) {
            if (groupId == -1L) {
                wrapper.isNull(TestSuite::getGroupId);
            } else {
                wrapper.eq(TestSuite::getGroupId, groupId);
            }
        }
        wrapper.orderByDesc(TestSuite::getCreatedAt);

        Page<TestSuite> result = testSuiteMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<SuiteResponse> records = new ArrayList<>(result.getRecords().size());
        for (TestSuite s : result.getRecords()) {
            records.add(toResponse(s, true));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 获取套件详情
     */
    public SuiteResponse getSuite(Long suiteId) {
        TestSuite suite = testSuiteMapper.selectById(suiteId);
        if (suite == null) {
            throw new BusinessException(ErrorCode.SUITE_NOT_FOUND, "测试套件不存在：" + suiteId);
        }
        return toResponse(suite, true);
    }

    /**
     * 创建测试套件
     */
    @Transactional(rollbackFor = Exception.class)
    public SuiteResponse createSuite(Long projectId, SuiteCreateRequest request) {
        // 名称唯一性校验
        if (countByName(projectId, request.getName(), null) > 0) {
            throw new BusinessException(ErrorCode.SUITE_NAME_DUPLICATE, "套件名称已存在：" + request.getName());
        }

        TestSuite suite = new TestSuite();
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
        testSuiteMapper.insert(suite);
        return toResponse(suite, false);
    }

    /**
     * 更新测试套件
     */
    @Transactional(rollbackFor = Exception.class)
    public SuiteResponse updateSuite(Long suiteId, SuiteUpdateRequest request) {
        TestSuite suite = testSuiteMapper.selectById(suiteId);
        if (suite == null) {
            throw new BusinessException(ErrorCode.SUITE_NOT_FOUND, "测试套件不存在：" + suiteId);
        }
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(suite.getName())) {
            if (countByName(suite.getProjectId(), request.getName(), suiteId) > 0) {
                throw new BusinessException(ErrorCode.SUITE_NAME_DUPLICATE, "套件名称已存在：" + request.getName());
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
        testSuiteMapper.updateById(suite);
        return toResponse(suite, true);
    }

    /**
     * 删除测试套件（用例由外键 ON DELETE CASCADE 级联删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSuite(Long suiteId) {
        TestSuite suite = testSuiteMapper.selectById(suiteId);
        if (suite == null) {
            throw new BusinessException(ErrorCode.SUITE_NOT_FOUND, "测试套件不存在：" + suiteId);
        }
        testSuiteMapper.deleteById(suiteId);
    }

    /**
     * 批量计算套件通过率
     *
     * <p>对每个套件：查询其下所有用例，取每条用例最近一次执行结果，
     * 计算 PASSED / 总数 作为通过率。
     */
    public List<SuitePassRateDTO> getPassRates(Long projectId, List<Long> suiteIds) {
        if (suiteIds == null || suiteIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SuitePassRateDTO> result = new ArrayList<>(suiteIds.size());
        for (Long suiteId : suiteIds) {
            SuitePassRateDTO dto = new SuitePassRateDTO();
            dto.setSuiteId(suiteId);

            // 查询套件下所有用例 ID
            LambdaQueryWrapper<TestCase> caseWrapper = new LambdaQueryWrapper<>();
            caseWrapper.eq(TestCase::getSuiteId, suiteId).select(TestCase::getId);
            List<TestCase> cases = testCaseMapper.selectList(caseWrapper);

            if (cases.isEmpty()) {
                dto.setPassRate(-1);
                dto.setTotalCases(0);
                result.add(dto);
                continue;
            }

            Set<Long> caseIds = new HashSet<>();
            for (TestCase c : cases) {
                caseIds.add(c.getId());
            }

            // 查询这些用例的所有执行结果，按 caseId + finishedAt 降序
            LambdaQueryWrapper<TestResult> resultWrapper = new LambdaQueryWrapper<>();
            resultWrapper.in(TestResult::getCaseId, caseIds);
            resultWrapper.isNotNull(TestResult::getFinishedAt);
            resultWrapper.orderByDesc(TestResult::getFinishedAt);
            List<TestResult> results = testResultMapper.selectList(resultWrapper);

            // 取每个用例的最新结果
            Map<Long, String> latestStatusMap = new LinkedHashMap<>();
            for (TestResult tr : results) {
                if (!latestStatusMap.containsKey(tr.getCaseId())) {
                    latestStatusMap.put(tr.getCaseId(), tr.getStatus());
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
     * 批量修改套件分组
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateGroup(List<Long> suiteIds, Long groupId) {
        if (suiteIds == null || suiteIds.isEmpty()) {
            return;
        }
        for (Long suiteId : suiteIds) {
            TestSuite suite = testSuiteMapper.selectById(suiteId);
            if (suite != null) {
                suite.setGroupId(groupId);
                testSuiteMapper.updateById(suite);
            }
        }
    }

    private long countByName(Long projectId, String name, Long excludeId) {
        LambdaQueryWrapper<TestSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestSuite::getProjectId, projectId);
        wrapper.eq(TestSuite::getName, name);
        if (excludeId != null) {
            wrapper.ne(TestSuite::getId, excludeId);
        }
        return testSuiteMapper.selectCount(wrapper);
    }

    private SuiteResponse toResponse(TestSuite suite, boolean withCaseCount) {
        SuiteResponse r = new SuiteResponse();
        BeanUtils.copyProperties(suite, r);
        if (withCaseCount) {
            LambdaQueryWrapper<TestCase> w = new LambdaQueryWrapper<>();
            w.eq(TestCase::getSuiteId, suite.getId());
            r.setCaseCount(testCaseMapper.selectCount(w));
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
