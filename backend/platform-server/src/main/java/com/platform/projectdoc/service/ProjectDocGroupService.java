/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档分组管理服务
 */
package com.platform.projectdoc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.project.service.ProjectService;
import com.platform.projectdoc.dto.ProjectDocGroupCreateRequest;
import com.platform.projectdoc.dto.ProjectDocGroupResponse;
import com.platform.projectdoc.dto.ProjectDocGroupUpdateRequest;
import com.platform.projectdoc.entity.ProjectDoc;
import com.platform.projectdoc.entity.ProjectDocGroup;
import com.platform.projectdoc.mapper.ProjectDocGroupMapper;
import com.platform.projectdoc.mapper.ProjectDocMapper;
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
 * 项目文档分组管理服务
 */
@Service
@RequiredArgsConstructor
public class ProjectDocGroupService {

    private final ProjectDocGroupMapper projectDocGroupMapper;
    private final ProjectDocMapper projectDocMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的分组列表（扁平列表，前端自行建树）
     * <p>docCount 包含子分组的文档数（自底向上聚合）。
     */
    public List<ProjectDocGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<ProjectDocGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectDocGroup::getProjectId, projectId);
        wrapper.orderByDesc(ProjectDocGroup::getIsSystem, ProjectDocGroup::getCreatedAt);

        List<ProjectDocGroup> list = projectDocGroupMapper.selectList(wrapper);

        // 统计每个分组的直接文档数
        Map<Long, Integer> directCountMap = new LinkedHashMap<>();
        for (ProjectDocGroup group : list) {
            LambdaQueryWrapper<ProjectDoc> docWrapper = new LambdaQueryWrapper<>();
            docWrapper.eq(ProjectDoc::getGroupId, group.getId());
            directCountMap.put(group.getId(), projectDocMapper.selectCount(docWrapper).intValue());
        }

        // 建树后自底向上聚合子分组文档数
        Map<Long, List<ProjectDocGroup>> childrenMap = list.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(ProjectDocGroup::getParentId));

        Map<Long, Integer> totalCountMap = new LinkedHashMap<>();
        for (ProjectDocGroup group : list) {
            totalCountMap.put(group.getId(), aggregateCount(group.getId(), directCountMap, childrenMap));
        }

        List<ProjectDocGroupResponse> result = new ArrayList<>();
        for (ProjectDocGroup group : list) {
            ProjectDocGroupResponse resp = toResponse(group);
            resp.setDocCount(totalCountMap.getOrDefault(group.getId(), 0));
            result.add(resp);
        }
        return result;
    }

    /**
     * 获取指定分组及其所有子孙分组的 ID 集合（用于文档列表过滤）
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
    public ProjectDocGroupResponse create(ProjectDocGroupCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        ProjectDocGroup group = new ProjectDocGroup();
        group.setProjectId(request.getProjectId());
        group.setParentId(request.getParentId());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setIsSystem(0);
        group.setCreatedBy(getCurrentUserId());

        projectDocGroupMapper.insert(group);
        return toResponse(group);
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectDocGroupResponse update(Long groupId, ProjectDocGroupUpdateRequest request) {
        ProjectDocGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.PROJECT_DOC_GROUP_SYSTEM, "系统分组不允许修改");
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

        projectDocGroupMapper.updateById(group);
        return toResponse(group);
    }

    /**
     * 删除分组（系统分组不允许删除）
     * <p>删除时将该分组及其子分组下的文档 groupId 设为 NULL（归入未分组）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        ProjectDocGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.PROJECT_DOC_GROUP_SYSTEM, "系统分组不允许删除");
        }

        // 收集该分组及所有子孙分组 ID
        Set<Long> descendantIds = getDescendantGroupIds(groupId);

        // 将这些分组下的文档 groupId 设为 NULL
        for (Long gid : descendantIds) {
            LambdaQueryWrapper<ProjectDoc> docWrapper = new LambdaQueryWrapper<>();
            docWrapper.eq(ProjectDoc::getGroupId, gid);
            List<ProjectDoc> docs = projectDocMapper.selectList(docWrapper);
            for (ProjectDoc doc : docs) {
                doc.setGroupId(null);
                projectDocMapper.updateById(doc);
            }
        }

        // 删除子孙分组（从叶子到根）
        for (Long gid : descendantIds) {
            if (!gid.equals(groupId)) {
                projectDocGroupMapper.deleteById(gid);
            }
        }
        projectDocGroupMapper.deleteById(groupId);
    }

    // ───────────────────── 私有方法 ─────────────────────

    /**
     * 递归聚合分组及其子分组的文档数
     */
    private int aggregateCount(Long groupId, Map<Long, Integer> directCountMap,
                               Map<Long, List<ProjectDocGroup>> childrenMap) {
        int count = directCountMap.getOrDefault(groupId, 0);
        List<ProjectDocGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (ProjectDocGroup child : children) {
                count += aggregateCount(child.getId(), directCountMap, childrenMap);
            }
        }
        return count;
    }

    /**
     * 递归收集子孙分组 ID
     */
    private void collectDescendants(Long parentId, Set<Long> collected) {
        LambdaQueryWrapper<ProjectDocGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectDocGroup::getParentId, parentId);
        List<ProjectDocGroup> children = projectDocGroupMapper.selectList(wrapper);
        for (ProjectDocGroup child : children) {
            collected.add(child.getId());
            collectDescendants(child.getId(), collected);
        }
    }

    private ProjectDocGroup findById(Long groupId) {
        ProjectDocGroup group = projectDocGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.PROJECT_DOC_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }
        return group;
    }

    private ProjectDocGroupResponse toResponse(ProjectDocGroup group) {
        ProjectDocGroupResponse resp = new ProjectDocGroupResponse();
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
