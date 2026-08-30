/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求-用例关联管理服务
 */
package com.platform.requirement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.auth.mapper.UserMapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.execution.entity.ManualCase;
import com.platform.execution.entity.TestCase;
import com.platform.execution.entity.TestSuite;
import com.platform.execution.mapper.ManualCaseMapper;
import com.platform.execution.mapper.TestCaseMapper;
import com.platform.execution.mapper.TestSuiteMapper;
import com.platform.project.service.ProjectService;
import com.platform.requirement.dto.RequirementCaseRelationCreateRequest;
import com.platform.requirement.dto.RequirementCaseRelationResponse;
import com.platform.requirement.entity.RequirementCaseRelation;
import com.platform.requirement.entity.RequirementItem;
import com.platform.requirement.entity.RequirementVersion;
import com.platform.requirement.mapper.RequirementCaseRelationMapper;
import com.platform.requirement.mapper.RequirementItemMapper;
import com.platform.requirement.mapper.RequirementVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 需求-用例关联管理服务
 *
 * <p>维护需求条目与用例（手动/自动）的多对多关联。
 * 正查：需求条目 → 关联用例列表；反查：用例 → 关联需求条目列表。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequirementCaseRelationService {

    /**
     * 用例类型：手动用例
     */
    public static final String CASE_TYPE_MANUAL = "MANUAL_CASE";

    /**
     * 用例类型：自动用例
     */
    public static final String CASE_TYPE_AUTO = "TEST_CASE";

    private final RequirementCaseRelationMapper relationMapper;
    private final RequirementItemMapper itemMapper;
    private final RequirementVersionMapper versionMapper;
    private final ManualCaseMapper manualCaseMapper;
    private final TestCaseMapper testCaseMapper;
    private final TestSuiteMapper testSuiteMapper;
    private final UserMapper userMapper;
    private final ProjectService projectService;

    /**
     * 查询需求条目下关联的用例列表（正查）
     */
    public List<RequirementCaseRelationResponse> listByItem(Long itemId) {
        findItemById(itemId);
        LambdaQueryWrapper<RequirementCaseRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementCaseRelation::getRequirementItemId, itemId)
                .orderByDesc(RequirementCaseRelation::getCreatedAt);
        return relationMapper.selectList(wrapper).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 查询用例关联的需求条目列表（反查）
     */
    public List<RequirementCaseRelationResponse> listByCase(Long projectId, String caseType, Long caseId) {
        projectService.findActiveById(projectId);
        validateCaseType(caseType);

        LambdaQueryWrapper<RequirementCaseRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementCaseRelation::getCaseType, caseType)
                .eq(RequirementCaseRelation::getCaseId, caseId)
                .orderByDesc(RequirementCaseRelation::getCreatedAt);
        List<RequirementCaseRelation> relations = relationMapper.selectList(wrapper);

        List<RequirementCaseRelationResponse> result = new ArrayList<>(relations.size());
        for (RequirementCaseRelation relation : relations) {
            RequirementCaseRelationResponse resp = toResponse(relation);
            fillRequirementInfo(resp);
            result.add(resp);
        }
        return result;
    }

    /**
     * 添加关联（需求条目 → 用例）
     *
     * <p>校验条目存在、用例存在且与需求条目同项目（自动用例经所属套件归属项目），
     * 防止跨项目关联；唯一键预检防重复。
     */
    @Transactional(rollbackFor = Exception.class)
    public RequirementCaseRelationResponse addRelation(Long itemId, RequirementCaseRelationCreateRequest request) {
        RequirementItem item = findItemById(itemId);
        String caseType = request.getCaseType();
        validateCaseType(caseType);

        // 校验用例存在并归属同一项目，回填标题快照
        String caseTitle;
        if (CASE_TYPE_MANUAL.equals(caseType)) {
            ManualCase manualCase = manualCaseMapper.selectById(request.getCaseId());
            if (manualCase == null) {
                throw new BusinessException(ErrorCode.MANUAL_CASE_NOT_FOUND, "手动用例不存在：" + request.getCaseId());
            }
            if (!Objects.equals(manualCase.getProjectId(), getProjectIdByItem(item))) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "用例与需求条目不属于同一项目");
            }
            caseTitle = manualCase.getTitle();
        } else {
            TestCase testCase = testCaseMapper.selectById(request.getCaseId());
            if (testCase == null) {
                throw new BusinessException(ErrorCode.CASE_NOT_FOUND, "自动用例不存在：" + request.getCaseId());
            }
            TestSuite suite = testSuiteMapper.selectById(testCase.getSuiteId());
            if (suite == null || !Objects.equals(suite.getProjectId(), getProjectIdByItem(item))) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "用例与需求条目不属于同一项目");
            }
            caseTitle = testCase.getName();
        }

        // 防重复
        LambdaQueryWrapper<RequirementCaseRelation> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(RequirementCaseRelation::getRequirementItemId, itemId)
                .eq(RequirementCaseRelation::getCaseType, caseType)
                .eq(RequirementCaseRelation::getCaseId, request.getCaseId());
        if (relationMapper.selectCount(dupWrapper) > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "该用例已关联到当前需求条目");
        }

        RequirementCaseRelation relation = new RequirementCaseRelation();
        relation.setRequirementItemId(itemId);
        relation.setCaseType(caseType);
        relation.setCaseId(request.getCaseId());
        relation.setCaseTitle(caseTitle);
        relation.setCreatedBy(getCurrentUserId());
        relationMapper.insert(relation);
        return toResponse(relation);
    }

    /**
     * 删除关联
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRelation(Long relationId) {
        relationMapper.deleteById(relationId);
    }

    /**
     * 清理需求条目下的全部关联（供删除条目/版本时级联调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByItem(Long itemId) {
        LambdaQueryWrapper<RequirementCaseRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementCaseRelation::getRequirementItemId, itemId);
        relationMapper.delete(wrapper);
    }

    /**
     * 清理某用例的全部关联（供删除用例时级联调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByCase(String caseType, Long caseId) {
        LambdaQueryWrapper<RequirementCaseRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementCaseRelation::getCaseType, caseType)
                .eq(RequirementCaseRelation::getCaseId, caseId);
        relationMapper.delete(wrapper);
    }

    // ───────────────────── 私有方法 ─────────────────────

    private RequirementItem findItemById(Long itemId) {
        RequirementItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "需求条目不存在");
        }
        return item;
    }

    /**
     * 经需求条目 → 版本 → 项目，解析所属项目 ID
     */
    private Long getProjectIdByItem(RequirementItem item) {
        RequirementVersion version = versionMapper.selectById(item.getVersionId());
        if (version == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "需求条目所属版本不存在");
        }
        return version.getProjectId();
    }

    /**
     * 反查时填充需求条目标题、状态及所属版本信息
     */
    private void fillRequirementInfo(RequirementCaseRelationResponse resp) {
        RequirementItem item = itemMapper.selectById(resp.getRequirementItemId());
        if (item == null) {
            return;
        }
        resp.setRequirementItemTitle(item.getTitle());
        resp.setRequirementItemStatus(item.getStatus());
        resp.setVersionId(item.getVersionId());
        RequirementVersion version = versionMapper.selectById(item.getVersionId());
        if (version != null) {
            resp.setVersionName(version.getVersionName());
        }
    }

    private void validateCaseType(String caseType) {
        if (!CASE_TYPE_MANUAL.equals(caseType) && !CASE_TYPE_AUTO.equals(caseType)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "无效的用例类型：" + caseType);
        }
    }

    private RequirementCaseRelationResponse toResponse(RequirementCaseRelation relation) {
        RequirementCaseRelationResponse resp = new RequirementCaseRelationResponse();
        resp.setId(relation.getId());
        resp.setRequirementItemId(relation.getRequirementItemId());
        resp.setCaseType(relation.getCaseType());
        resp.setCaseId(relation.getCaseId());
        resp.setCaseTitle(relation.getCaseTitle());
        resp.setCreatedBy(relation.getCreatedBy());
        resp.setCreatedByName(getUserName(relation.getCreatedBy()));
        resp.setCreatedAt(relation.getCreatedAt());
        return resp;
    }

    private String getUserName(Long userId) {
        if (userId == null) return null;
        User user = userMapper.selectById(userId);
        return user != null ? user.getDisplayName() : null;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }
}
