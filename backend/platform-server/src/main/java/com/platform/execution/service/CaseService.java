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
import com.platform.execution.dto.CaseResponse;
import com.platform.execution.dto.CaseUpdateRequest;
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
import java.util.Set;

/**
 * 测试用例服务
 */
@Service
@RequiredArgsConstructor
public class CaseService {

    private final TestCaseMapper testCaseMapper;
    private final TestSuiteMapper testSuiteMapper;
    private final CaseGroupService caseGroupService;

    /**
     * 分页查询测试用例
     */
    public PageResponse<CaseResponse> listCases(Long suiteId, Long groupId, String keyword,
                                                 String priority, String status, int page, int pageSize) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        if (suiteId != null) {
            wrapper.eq(TestCase::getSuiteId, suiteId);
        }
        // 按分组筛选（含子孙分组）
        if (groupId != null) {
            Set<Long> groupIds = caseGroupService.getDescendantGroupIds(groupId);
            wrapper.in(TestCase::getGroupId, groupIds);
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
            c.setPriority("P2");
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
     * 删除测试用例
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long caseId) {
        TestCase c = testCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND, "测试用例不存在：" + caseId);
        }
        testCaseMapper.deleteById(caseId);
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

    private long countByName(Long suiteId, String name, Long excludeId) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestCase::getSuiteId, suiteId);
        wrapper.eq(TestCase::getName, name);
        if (excludeId != null) {
            wrapper.ne(TestCase::getId, excludeId);
        }
        return testCaseMapper.selectCount(wrapper);
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
