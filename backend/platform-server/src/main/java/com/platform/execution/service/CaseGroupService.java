/**
 * @author HXN
 * @date 2026-08-23
 * @description 测试用例分组管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.execution.dto.CaseGroupCreateRequest;
import com.platform.execution.dto.CaseGroupResponse;
import com.platform.execution.dto.CaseGroupUpdateRequest;
import com.platform.execution.entity.CaseGroup;
import com.platform.execution.entity.TestCase;
import com.platform.execution.mapper.CaseGroupMapper;
import com.platform.execution.mapper.TestCaseMapper;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 测试用例分组管理服务
 */
@Service
@RequiredArgsConstructor
public class CaseGroupService {

    private final CaseGroupMapper caseGroupMapper;
    private final TestCaseMapper testCaseMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的分组列表（扁平列表，前端自行建树）
     * <p>caseCount 包含子分组的用例数（自底向上聚合）。
     */
    public List<CaseGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<CaseGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseGroup::getProjectId, projectId);
        wrapper.orderByDesc(CaseGroup::getIsSystem, CaseGroup::getCreatedAt);

        List<CaseGroup> list = caseGroupMapper.selectList(wrapper);

        // 统计每个分组的直接用例数
        Map<Long, Integer> directCountMap = new LinkedHashMap<>();
        for (CaseGroup group : list) {
            LambdaQueryWrapper<TestCase> caseWrapper = new LambdaQueryWrapper<>();
            caseWrapper.eq(TestCase::getGroupId, group.getId());
            directCountMap.put(group.getId(), testCaseMapper.selectCount(caseWrapper).intValue());
        }

        // 建树后自底向上聚合子分组用例数
        Map<Long, List<CaseGroup>> childrenMap = list.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(CaseGroup::getParentId));

        Map<Long, Integer> totalCountMap = new LinkedHashMap<>();
        for (CaseGroup group : list) {
            totalCountMap.put(group.getId(), aggregateCount(group.getId(), directCountMap, childrenMap));
        }

        List<CaseGroupResponse> result = new ArrayList<>();
        for (CaseGroup group : list) {
            CaseGroupResponse resp = toResponse(group);
            resp.setCaseCount(totalCountMap.getOrDefault(group.getId(), 0));
            result.add(resp);
        }
        return result;
    }

    /**
     * 获取指定分组及其所有子孙分组的 ID 集合（用于用例列表过滤）
     */
    public Set<Long> getDescendantGroupIds(Long groupId) {
        Set<Long> result = new LinkedHashSet<>();
        result.add(groupId);
        collectDescendants(groupId, result);
        return result;
    }

    /**
     * 创建分组
     */
    @Transactional(rollbackFor = Exception.class)
    public CaseGroupResponse create(CaseGroupCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        CaseGroup group = new CaseGroup();
        group.setProjectId(request.getProjectId());
        group.setParentId(request.getParentId());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setIsSystem(0);
        group.setCreatedBy(getCurrentUserId());

        caseGroupMapper.insert(group);
        return toResponse(group);
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public CaseGroupResponse update(Long groupId, CaseGroupUpdateRequest request) {
        CaseGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.CASE_GROUP_SYSTEM, "系统分组不允许修改");
        }

        if (StringUtils.hasText(request.getName())) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getParentId() != null) {
            group.setParentId(request.getParentId());
        }

        caseGroupMapper.updateById(group);
        return toResponse(group);
    }

    /**
     * 删除分组（系统分组不允许删除）
     * <p>删除时将该分组及其子分组下的用例 groupId 设为 NULL（归入未分组）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        CaseGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.CASE_GROUP_SYSTEM, "系统分组不允许删除");
        }

        // 收集该分组及所有子孙分组 ID
        Set<Long> descendantIds = getDescendantGroupIds(groupId);

        // 将这些分组下的用例 groupId 设为 NULL
        for (Long gid : descendantIds) {
            LambdaQueryWrapper<TestCase> caseWrapper = new LambdaQueryWrapper<>();
            caseWrapper.eq(TestCase::getGroupId, gid);
            List<TestCase> cases = testCaseMapper.selectList(caseWrapper);
            for (TestCase c : cases) {
                c.setGroupId(null);
                testCaseMapper.updateById(c);
            }
        }

        // 删除子孙分组（从叶子到根）
        for (Long gid : descendantIds) {
            if (!gid.equals(groupId)) {
                caseGroupMapper.deleteById(gid);
            }
        }
        caseGroupMapper.deleteById(groupId);
    }

    // ───────────────────── 私有方法 ─────────────────────

    /**
     * 递归聚合分组及其子分组的用例数
     */
    private int aggregateCount(Long groupId, Map<Long, Integer> directCountMap,
                               Map<Long, List<CaseGroup>> childrenMap) {
        int count = directCountMap.getOrDefault(groupId, 0);
        List<CaseGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (CaseGroup child : children) {
                count += aggregateCount(child.getId(), directCountMap, childrenMap);
            }
        }
        return count;
    }

    /**
     * 递归收集子孙分组 ID
     */
    private void collectDescendants(Long parentId, Set<Long> collected) {
        LambdaQueryWrapper<CaseGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseGroup::getParentId, parentId);
        List<CaseGroup> children = caseGroupMapper.selectList(wrapper);
        for (CaseGroup child : children) {
            collected.add(child.getId());
            collectDescendants(child.getId(), collected);
        }
    }

    private CaseGroup findById(Long groupId) {
        CaseGroup group = caseGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.CASE_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }
        return group;
    }

    private CaseGroupResponse toResponse(CaseGroup group) {
        CaseGroupResponse resp = new CaseGroupResponse();
        BeanUtils.copyProperties(group, resp);
        return resp;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }
}
