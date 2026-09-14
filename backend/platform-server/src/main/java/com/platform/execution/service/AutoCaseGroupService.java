/**
 * @author HXN
 * @date 2026-08-23
 * @description 自动化用例分组管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.execution.dto.AutoCaseGroupCreateRequest;
import com.platform.execution.dto.AutoCaseGroupResponse;
import com.platform.execution.dto.AutoCaseGroupUpdateRequest;
import com.platform.execution.entity.AutoCase;
import com.platform.execution.entity.AutoCaseGroup;
import com.platform.execution.mapper.AutoCaseGroupMapper;
import com.platform.execution.mapper.AutoCaseMapper;
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
 * 自动化用例分组管理服务
 */
@Service
@RequiredArgsConstructor
public class AutoCaseGroupService {

    private final AutoCaseGroupMapper autoCaseGroupMapper;
    private final AutoCaseMapper autoCaseMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的分组列表（扁平列表，前端自行建树）
     * <p>caseCount 包含子分组的自动化用例数（自底向上聚合）。
     */
    public List<AutoCaseGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<AutoCaseGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoCaseGroup::getProjectId, projectId);
        wrapper.orderByDesc(AutoCaseGroup::getIsSystem, AutoCaseGroup::getCreatedAt);

        List<AutoCaseGroup> list = autoCaseGroupMapper.selectList(wrapper);

        // 统计每个分组的直接自动化用例数
        Map<Long, Integer> directCountMap = new LinkedHashMap<>();
        for (AutoCaseGroup group : list) {
            LambdaQueryWrapper<AutoCase> caseWrapper = new LambdaQueryWrapper<>();
            caseWrapper.eq(AutoCase::getGroupId, group.getId());
            directCountMap.put(group.getId(), autoCaseMapper.selectCount(caseWrapper).intValue());
        }

        // 建树后自底向上聚合子分组的自动化用例数
        Map<Long, List<AutoCaseGroup>> childrenMap = list.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(AutoCaseGroup::getParentId));

        Map<Long, Integer> totalCountMap = new LinkedHashMap<>();
        for (AutoCaseGroup group : list) {
            totalCountMap.put(group.getId(), aggregateCount(group.getId(), directCountMap, childrenMap));
        }

        List<AutoCaseGroupResponse> result = new ArrayList<>();
        for (AutoCaseGroup group : list) {
            AutoCaseGroupResponse resp = toResponse(group);
            resp.setCaseCount(totalCountMap.getOrDefault(group.getId(), 0));
            result.add(resp);
        }
        return result;
    }

    /**
     * 获取指定分组及其所有子孙分组的 ID 集合（用于自动化用例列表过滤）
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
    public AutoCaseGroupResponse create(AutoCaseGroupCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        AutoCaseGroup group = new AutoCaseGroup();
        group.setProjectId(request.getProjectId());
        group.setParentId(request.getParentId());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setIsSystem(0);
        group.setCreatedBy(getCurrentUserId());

        autoCaseGroupMapper.insert(group);
        return toResponse(group);
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoCaseGroupResponse update(Long groupId, AutoCaseGroupUpdateRequest request) {
        AutoCaseGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.AUTO_CASE_GROUP_SYSTEM, "系统分组不允许修改");
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

        autoCaseGroupMapper.updateById(group);
        return toResponse(group);
    }

    /**
     * 删除分组（系统分组不允许删除）
     * <p>删除时将该分组及其子分组下的自动化用例 groupId 设为 NULL（归入未分组）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        AutoCaseGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.AUTO_CASE_GROUP_SYSTEM, "系统分组不允许删除");
        }

        // 收集该分组及所有子孙分组 ID
        Set<Long> descendantIds = getDescendantGroupIds(groupId);

        // 将这些分组下的自动化用例 groupId 设为 NULL
        for (Long gid : descendantIds) {
            LambdaQueryWrapper<AutoCase> caseWrapper = new LambdaQueryWrapper<>();
            caseWrapper.eq(AutoCase::getGroupId, gid);
            List<AutoCase> cases = autoCaseMapper.selectList(caseWrapper);
            for (AutoCase c : cases) {
                c.setGroupId(null);
                autoCaseMapper.updateById(c);
            }
        }

        // 删除子孙分组（从叶子到根）
        for (Long gid : descendantIds) {
            if (!gid.equals(groupId)) {
                autoCaseGroupMapper.deleteById(gid);
            }
        }
        autoCaseGroupMapper.deleteById(groupId);
    }

    // ───────────────────── 私有方法 ─────────────────────

    /**
     * 递归聚合分组及其子分组的自动化用例数
     */
    private int aggregateCount(Long groupId, Map<Long, Integer> directCountMap,
                               Map<Long, List<AutoCaseGroup>> childrenMap) {
        int count = directCountMap.getOrDefault(groupId, 0);
        List<AutoCaseGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (AutoCaseGroup child : children) {
                count += aggregateCount(child.getId(), directCountMap, childrenMap);
            }
        }
        return count;
    }

    /**
     * 递归收集子孙分组 ID
     */
    private void collectDescendants(Long parentId, Set<Long> collected) {
        LambdaQueryWrapper<AutoCaseGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoCaseGroup::getParentId, parentId);
        List<AutoCaseGroup> children = autoCaseGroupMapper.selectList(wrapper);
        for (AutoCaseGroup child : children) {
            collected.add(child.getId());
            collectDescendants(child.getId(), collected);
        }
    }

    private AutoCaseGroup findById(Long groupId) {
        AutoCaseGroup group = autoCaseGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.AUTO_CASE_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }
        return group;
    }

    private AutoCaseGroupResponse toResponse(AutoCaseGroup group) {
        AutoCaseGroupResponse resp = new AutoCaseGroupResponse();
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
