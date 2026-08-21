package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.execution.dto.SuiteCreateRequest;
import com.platform.execution.dto.SuiteResponse;
import com.platform.execution.dto.SuiteUpdateRequest;
import com.platform.execution.entity.TestCase;
import com.platform.execution.entity.TestSuite;
import com.platform.execution.mapper.TestCaseMapper;
import com.platform.execution.mapper.TestSuiteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试套件服务
 */
@Service
@RequiredArgsConstructor
public class SuiteService {

    private final TestSuiteMapper testSuiteMapper;
    private final TestCaseMapper testCaseMapper;

    /**
     * 分页查询测试套件
     */
    public PageResponse<SuiteResponse> listSuites(Long projectId, String keyword, int page, int pageSize) {
        LambdaQueryWrapper<TestSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestSuite::getProjectId, projectId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TestSuite::getName, keyword)
                    .or().like(TestSuite::getDescription, keyword));
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
