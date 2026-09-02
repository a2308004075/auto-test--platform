/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动化用例管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.auth.entity.User;
import com.platform.common.constant.BizType;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.common.service.ChangeLogService;
import com.platform.common.service.CommentService;
import com.platform.common.util.ChangeLogHelper;
import com.platform.execution.dto.ManualCaseCreateRequest;
import com.platform.execution.dto.ManualCaseResponse;
import com.platform.execution.dto.ManualCaseUpdateRequest;
import com.platform.execution.entity.DefectRelation;
import com.platform.execution.entity.ManualCase;
import com.platform.execution.mapper.DefectRelationMapper;
import com.platform.execution.mapper.ManualCaseMapper;
import com.platform.project.service.ProjectService;
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
import java.util.List;
import java.util.Set;

/**
 * 手动化用例管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManualCaseService {

    private final ManualCaseMapper manualCaseMapper;
    private final ManualCaseGroupService manualCaseGroupService;
    private final ProjectService projectService;
    private final ChangeLogService changeLogService;
    private final CommentService commentService;
    private final RequirementCaseRelationService requirementCaseRelationService;
    private final DefectRelationMapper defectRelationMapper;

    /**
     * 分页查询手动化用例
     *
     * @param projectId 项目 ID
     * @param groupId   分组 ID（null=不过滤，0=未分组，正数=指定分组含子孙分组）
     */
    public PageResponse<ManualCaseResponse> listCases(Long projectId, Long groupId, String keyword,
                                                       String priority, String caseType, String caseStatus,
                                                       int page, int pageSize) {
        projectService.findActiveById(projectId);

        LambdaQueryWrapper<ManualCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ManualCase::getProjectId, projectId);

        // 按分组筛选（0=未分组；正数=含子孙分组）
        if (groupId != null) {
            if (groupId == 0L) {
                wrapper.isNull(ManualCase::getGroupId);
            } else {
                Set<Long> groupIds = manualCaseGroupService.getDescendantGroupIds(groupId);
                wrapper.in(ManualCase::getGroupId, groupIds);
            }
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ManualCase::getTitle, keyword)
                    .or().like(ManualCase::getPreconditions, keyword)
                    .or().like(ManualCase::getOperationSteps, keyword));
        }
        if (StringUtils.hasText(priority)) {
            wrapper.eq(ManualCase::getPriority, priority);
        }
        if (StringUtils.hasText(caseType)) {
            wrapper.eq(ManualCase::getCaseType, caseType);
        }
        if (StringUtils.hasText(caseStatus)) {
            wrapper.eq(ManualCase::getCaseStatus, Integer.parseInt(caseStatus));
        }
        wrapper.orderByDesc(ManualCase::getCreatedAt);

        Page<ManualCase> result = manualCaseMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<ManualCaseResponse> records = new ArrayList<>(result.getRecords().size());
        for (ManualCase c : result.getRecords()) {
            records.add(toResponse(c));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 获取用例详情
     */
    public ManualCaseResponse getCase(Long caseId) {
        ManualCase c = manualCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.MANUAL_CASE_NOT_FOUND, "手动化用例不存在：" + caseId);
        }
        return toResponse(c);
    }

    /**
     * 创建手动化用例
     */
    @Transactional(rollbackFor = Exception.class)
    public ManualCaseResponse createCase(Long projectId, ManualCaseCreateRequest request) {
        projectService.findActiveById(projectId);

        ManualCase c = new ManualCase();
        BeanUtils.copyProperties(request, c);
        c.setProjectId(projectId);
        if (!StringUtils.hasText(c.getCaseType())) {
            c.setCaseType("NORMAL");
        }
        if (!StringUtils.hasText(c.getPriority())) {
            c.setPriority("中");
        }
        if (c.getRunInTestEnv() == null) {
            c.setRunInTestEnv(1);
        }
        if (c.getRunInProdEnv() == null) {
            c.setRunInProdEnv(0);
        }
        if (c.getCaseStatus() == null) {
            c.setCaseStatus(1);
        }
        c.setCreatedBy(getCurrentUserId());
        manualCaseMapper.insert(c);
        return toResponse(c);
    }

    /**
     * 更新手动化用例
     */
    @Transactional(rollbackFor = Exception.class)
    public ManualCaseResponse updateCase(Long caseId, ManualCaseUpdateRequest request) {
        ManualCase c = manualCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.MANUAL_CASE_NOT_FOUND, "手动化用例不存在：" + caseId);
        }

        // 记录变更前值
        String oldTitle = c.getTitle();
        String oldPreconditions = c.getPreconditions();
        String oldOperationSteps = c.getOperationSteps();
        String oldExpectedResult = c.getExpectedResult();
        String oldCaseType = c.getCaseType();
        String oldPriority = c.getPriority();
        Long oldGroupId = c.getGroupId();
        Integer oldRunInTestEnv = c.getRunInTestEnv();
        Integer oldRunInProdEnv = c.getRunInProdEnv();
        Integer oldCaseStatus = c.getCaseStatus();

        if (StringUtils.hasText(request.getTitle())) {
            c.setTitle(request.getTitle());
        }
        if (request.getPreconditions() != null) {
            c.setPreconditions(request.getPreconditions());
        }
        if (request.getOperationSteps() != null) {
            c.setOperationSteps(request.getOperationSteps());
        }
        if (request.getExpectedResult() != null) {
            c.setExpectedResult(request.getExpectedResult());
        }
        if (request.getCaseType() != null) {
            c.setCaseType(request.getCaseType());
        }
        if (request.getPriority() != null) {
            c.setPriority(request.getPriority());
        }
        if (request.getGroupId() != null) {
            c.setGroupId(request.getGroupId());
        }
        if (request.getRunInTestEnv() != null) {
            c.setRunInTestEnv(request.getRunInTestEnv());
        }
        if (request.getRunInProdEnv() != null) {
            c.setRunInProdEnv(request.getRunInProdEnv());
        }
        if (request.getCaseStatus() != null) {
            c.setCaseStatus(request.getCaseStatus());
        }
        manualCaseMapper.updateById(c);

        // 记录字段变更
        ChangeLogHelper.collect(BizType.MANUAL_CASE, caseId, changeLogService)
                .compare("title", oldTitle, c.getTitle())
                .compare("preconditions", oldPreconditions, c.getPreconditions())
                .compare("operationSteps", oldOperationSteps, c.getOperationSteps())
                .compare("expectedResult", oldExpectedResult, c.getExpectedResult())
                .compare("caseType", oldCaseType, c.getCaseType())
                .compare("priority", oldPriority, c.getPriority())
                .compare("groupId", oldGroupId, c.getGroupId())
                .compare("runInTestEnv", oldRunInTestEnv, c.getRunInTestEnv())
                .compare("runInProdEnv", oldRunInProdEnv, c.getRunInProdEnv())
                .compare("caseStatus", oldCaseStatus, c.getCaseStatus())
                .save();

        return toResponse(c);
    }

    /**
     * 删除手动化用例（同步清理评论、变更记录、需求关联与缺陷关联）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long caseId) {
        ManualCase c = manualCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.MANUAL_CASE_NOT_FOUND, "手动化用例不存在：" + caseId);
        }
        commentService.deleteByBiz(BizType.MANUAL_CASE, caseId);
        changeLogService.deleteByBiz(BizType.MANUAL_CASE, caseId);
        requirementCaseRelationService.deleteByCase(RequirementCaseRelationService.CASE_TYPE_MANUAL, caseId);
        deleteDefectRelations(caseId);
        manualCaseMapper.deleteById(caseId);
    }

    /**
     * 启用/废弃手动化用例
     */
    @Transactional(rollbackFor = Exception.class)
    public ManualCaseResponse toggleStatus(Long caseId) {
        ManualCase c = manualCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.MANUAL_CASE_NOT_FOUND, "手动化用例不存在：" + caseId);
        }
        Integer oldCaseStatus = c.getCaseStatus();
        c.setCaseStatus(Integer.valueOf(1).equals(c.getCaseStatus()) ? 0 : 1);
        manualCaseMapper.updateById(c);

        // 记录状态变更
        ChangeLogHelper.collect(BizType.MANUAL_CASE, caseId, changeLogService)
                .compare("caseStatus", oldCaseStatus, c.getCaseStatus())
                .save();

        return toResponse(c);
    }

    /**
     * 清空分组及其子孙分组中的所有手动化用例（同步清理评论与变更记录）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByGroup(Long projectId, Long groupId) {
        LambdaQueryWrapper<ManualCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ManualCase::getProjectId, projectId);
        if (groupId == 0L) {
            wrapper.isNull(ManualCase::getGroupId);
        } else {
            wrapper.in(ManualCase::getGroupId, manualCaseGroupService.getDescendantGroupIds(groupId));
        }
        deleteWithRelations(wrapper);
    }

    /**
     * 清空项目下所有手动化用例（同步清理评论与变更记录）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearByProject(Long projectId) {
        LambdaQueryWrapper<ManualCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ManualCase::getProjectId, projectId);
        deleteWithRelations(wrapper);
    }

    private void deleteWithRelations(LambdaQueryWrapper<ManualCase> wrapper) {
        List<ManualCase> cases = manualCaseMapper.selectList(wrapper);
        for (ManualCase c : cases) {
            commentService.deleteByBiz(BizType.MANUAL_CASE, c.getId());
            changeLogService.deleteByBiz(BizType.MANUAL_CASE, c.getId());
            requirementCaseRelationService.deleteByCase(RequirementCaseRelationService.CASE_TYPE_MANUAL, c.getId());
            deleteDefectRelations(c.getId());
        }
        manualCaseMapper.delete(wrapper);
    }

    /**
     * 删除某手动化用例的缺陷关联记录
     */
    private void deleteDefectRelations(Long caseId) {
        LambdaQueryWrapper<DefectRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DefectRelation::getTargetType, "MANUAL_CASE")
                .eq(DefectRelation::getTargetId, caseId);
        defectRelationMapper.delete(wrapper);
    }

    // ───────────────────── 私有方法 ─────────────────────

    private ManualCaseResponse toResponse(ManualCase c) {
        ManualCaseResponse r = new ManualCaseResponse();
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
