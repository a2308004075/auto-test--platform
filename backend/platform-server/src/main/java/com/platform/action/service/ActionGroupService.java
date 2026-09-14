/**
 * @author HXN
 * @date 2026-08-24
 * @description Action 关键字分组管理服务
 */
package com.platform.action.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.action.dto.ActionGroupCreateRequest;
import com.platform.action.dto.ActionGroupResponse;
import com.platform.action.dto.ActionGroupUpdateRequest;
import com.platform.action.entity.Action;
import com.platform.action.entity.ActionGroup;
import com.platform.action.mapper.ActionGroupMapper;
import com.platform.action.mapper.ActionMapper;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
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
 * Action 关键字分组管理服务
 */
@Service
@RequiredArgsConstructor
public class ActionGroupService {

    private final ActionGroupMapper actionGroupMapper;
    private final ActionMapper actionMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的分组列表（扁平列表，前端自行建树）
     * <p>actionCount 包含子分组的 Action 数（自底向上聚合）。
     */
    public List<ActionGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<ActionGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActionGroup::getProjectId, projectId);
        wrapper.orderByDesc(ActionGroup::getIsSystem, ActionGroup::getCreatedAt);

        List<ActionGroup> list = actionGroupMapper.selectList(wrapper);

        // 统计每个分组的直接 Action 数
        Map<Long, Integer> directCountMap = new LinkedHashMap<>();
        for (ActionGroup group : list) {
            LambdaQueryWrapper<Action> actionWrapper = new LambdaQueryWrapper<>();
            actionWrapper.eq(Action::getGroupId, group.getId());
            directCountMap.put(group.getId(), actionMapper.selectCount(actionWrapper).intValue());
        }

        // 建树后自底向上聚合子分组 Action 数
        Map<Long, List<ActionGroup>> childrenMap = list.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(ActionGroup::getParentId));

        Map<Long, Integer> totalCountMap = new LinkedHashMap<>();
        for (ActionGroup group : list) {
            totalCountMap.put(group.getId(), aggregateCount(group.getId(), directCountMap, childrenMap));
        }

        List<ActionGroupResponse> result = new ArrayList<>();
        for (ActionGroup group : list) {
            ActionGroupResponse resp = toResponse(group);
            resp.setActionCount(totalCountMap.getOrDefault(group.getId(), 0));
            result.add(resp);
        }
        return result;
    }

    /**
     * 获取指定分组及其所有子孙分组的 ID 集合（用于 Action 列表过滤）
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
    public ActionGroupResponse create(ActionGroupCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        ActionGroup group = new ActionGroup();
        group.setProjectId(request.getProjectId());
        group.setParentId(request.getParentId());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setIsSystem(0);
        group.setCreatedBy(getCurrentUserId());

        actionGroupMapper.insert(group);
        return toResponse(group);
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public ActionGroupResponse update(Long groupId, ActionGroupUpdateRequest request) {
        ActionGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.ACTION_GROUP_SYSTEM, "系统分组不允许修改");
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

        actionGroupMapper.updateById(group);
        return toResponse(group);
    }

    /**
     * 删除分组（系统分组不允许删除）
     * <p>删除时将该分组及其子分组下的 Action groupId 设为 NULL（归入未分组）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        ActionGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.ACTION_GROUP_SYSTEM, "系统分组不允许删除");
        }

        // 收集该分组及所有子孙分组 ID
        Set<Long> descendantIds = getDescendantGroupIds(groupId);

        // 将这些分组下的 Action groupId 设为 NULL
        for (Long gid : descendantIds) {
            LambdaQueryWrapper<Action> actionWrapper = new LambdaQueryWrapper<>();
            actionWrapper.eq(Action::getGroupId, gid);
            List<Action> actions = actionMapper.selectList(actionWrapper);
            for (Action a : actions) {
                a.setGroupId(null);
                actionMapper.updateById(a);
            }
        }

        // 删除子孙分组（从叶子到根）
        for (Long gid : descendantIds) {
            if (!gid.equals(groupId)) {
                actionGroupMapper.deleteById(gid);
            }
        }
        actionGroupMapper.deleteById(groupId);
    }

    // ───────────────────── 私有方法 ─────────────────────

    /**
     * 递归聚合分组及其子分组的 Action 数
     */
    private int aggregateCount(Long groupId, Map<Long, Integer> directCountMap,
                               Map<Long, List<ActionGroup>> childrenMap) {
        int count = directCountMap.getOrDefault(groupId, 0);
        List<ActionGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (ActionGroup child : children) {
                count += aggregateCount(child.getId(), directCountMap, childrenMap);
            }
        }
        return count;
    }

    /**
     * 递归收集子孙分组 ID
     */
    private void collectDescendants(Long parentId, Set<Long> collected) {
        LambdaQueryWrapper<ActionGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActionGroup::getParentId, parentId);
        List<ActionGroup> children = actionGroupMapper.selectList(wrapper);
        for (ActionGroup child : children) {
            collected.add(child.getId());
            collectDescendants(child.getId(), collected);
        }
    }

    private ActionGroup findById(Long groupId) {
        ActionGroup group = actionGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.ACTION_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }
        return group;
    }

    private ActionGroupResponse toResponse(ActionGroup group) {
        ActionGroupResponse resp = new ActionGroupResponse();
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
