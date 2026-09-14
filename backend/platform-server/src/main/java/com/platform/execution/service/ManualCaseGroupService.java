/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动化用例分组管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.execution.dto.ManualCaseGroupCreateRequest;
import com.platform.execution.dto.ManualCaseGroupResponse;
import com.platform.execution.dto.ManualCaseGroupUpdateRequest;
import com.platform.execution.entity.ManualCase;
import com.platform.execution.entity.ManualCaseGroup;
import com.platform.execution.mapper.ManualCaseGroupMapper;
import com.platform.execution.mapper.ManualCaseMapper;
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
 * 手动化用例分组管理服务
 */
@Service
@RequiredArgsConstructor
public class ManualCaseGroupService {

    private final ManualCaseGroupMapper manualCaseGroupMapper;
    private final ManualCaseMapper manualCaseMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的分组列表（扁平列表，前端自行建树）
     * <p>caseCount 包含子分组的用例数（自底向上聚合）。
     */
    public List<ManualCaseGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<ManualCaseGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ManualCaseGroup::getProjectId, projectId);
        wrapper.orderByDesc(ManualCaseGroup::getIsSystem, ManualCaseGroup::getCreatedAt);

        List<ManualCaseGroup> list = manualCaseGroupMapper.selectList(wrapper);

        // 统计每个分组的直接用例数
        Map<Long, Integer> directCountMap = new LinkedHashMap<>();
        for (ManualCaseGroup group : list) {
            LambdaQueryWrapper<ManualCase> caseWrapper = new LambdaQueryWrapper<>();
            caseWrapper.eq(ManualCase::getGroupId, group.getId());
            directCountMap.put(group.getId(), manualCaseMapper.selectCount(caseWrapper).intValue());
        }

        // 建树后自底向上聚合子分组用例数
        Map<Long, List<ManualCaseGroup>> childrenMap = list.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(ManualCaseGroup::getParentId));

        Map<Long, Integer> totalCountMap = new LinkedHashMap<>();
        for (ManualCaseGroup group : list) {
            totalCountMap.put(group.getId(), aggregateCount(group.getId(), directCountMap, childrenMap));
        }

        List<ManualCaseGroupResponse> result = new ArrayList<>();
        for (ManualCaseGroup group : list) {
            ManualCaseGroupResponse resp = toResponse(group);
            resp.setCaseCount(totalCountMap.getOrDefault(group.getId(), 0));
            result.add(resp);
        }
        return result;
    }

    /**
     * 获取指定分组及其所有子孙分组的 ID 集合
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
    public ManualCaseGroupResponse create(ManualCaseGroupCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        ManualCaseGroup group = new ManualCaseGroup();
        group.setProjectId(request.getProjectId());
        group.setParentId(request.getParentId());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setIsSystem(0);
        group.setCreatedBy(getCurrentUserId());

        manualCaseGroupMapper.insert(group);
        return toResponse(group);
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public ManualCaseGroupResponse update(Long groupId, ManualCaseGroupUpdateRequest request) {
        ManualCaseGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.MANUAL_CASE_GROUP_SYSTEM, "系统分组不允许修改");
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

        manualCaseGroupMapper.updateById(group);
        return toResponse(group);
    }

    /**
     * 删除分组（系统分组不允许删除）
     * <p>删除时将该分组及其子分组下的用例 groupId 设为 NULL（归入未分组）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        ManualCaseGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.MANUAL_CASE_GROUP_SYSTEM, "系统分组不允许删除");
        }

        Set<Long> descendantIds = getDescendantGroupIds(groupId);

        for (Long gid : descendantIds) {
            LambdaQueryWrapper<ManualCase> caseWrapper = new LambdaQueryWrapper<>();
            caseWrapper.eq(ManualCase::getGroupId, gid);
            List<ManualCase> cases = manualCaseMapper.selectList(caseWrapper);
            for (ManualCase c : cases) {
                c.setGroupId(null);
                manualCaseMapper.updateById(c);
            }
        }

        for (Long gid : descendantIds) {
            if (!gid.equals(groupId)) {
                manualCaseGroupMapper.deleteById(gid);
            }
        }
        manualCaseGroupMapper.deleteById(groupId);
    }

    // ───────────────────── 私有方法 ─────────────────────

    private int aggregateCount(Long groupId, Map<Long, Integer> directCountMap,
                               Map<Long, List<ManualCaseGroup>> childrenMap) {
        int count = directCountMap.getOrDefault(groupId, 0);
        List<ManualCaseGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (ManualCaseGroup child : children) {
                count += aggregateCount(child.getId(), directCountMap, childrenMap);
            }
        }
        return count;
    }

    private void collectDescendants(Long parentId, Set<Long> collected) {
        LambdaQueryWrapper<ManualCaseGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ManualCaseGroup::getParentId, parentId);
        List<ManualCaseGroup> children = manualCaseGroupMapper.selectList(wrapper);
        for (ManualCaseGroup child : children) {
            collected.add(child.getId());
            collectDescendants(child.getId(), collected);
        }
    }

    private ManualCaseGroup findById(Long groupId) {
        ManualCaseGroup group = manualCaseGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.MANUAL_CASE_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }
        return group;
    }

    private ManualCaseGroupResponse toResponse(ManualCaseGroup group) {
        ManualCaseGroupResponse resp = new ManualCaseGroupResponse();
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
