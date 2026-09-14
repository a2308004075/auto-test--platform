/**
 * @author HXN
 * @date 2026-09-14
 * @description 代码仓库分组管理服务
 */
package com.platform.repository.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.project.service.ProjectService;
import com.platform.repository.dto.RepositoryGroupCreateRequest;
import com.platform.repository.dto.RepositoryGroupResponse;
import com.platform.repository.dto.RepositoryGroupUpdateRequest;
import com.platform.repository.entity.CodeRepository;
import com.platform.repository.entity.CodeRepositoryGroup;
import com.platform.repository.mapper.CodeRepositoryGroupMapper;
import com.platform.repository.mapper.CodeRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代码仓库分组管理服务
 *
 * <p>对齐接口文档模块 ApiModuleService 的行为：
 * 树形分组（parentId）、系统分组保护、repoCount 自底向上聚合、
 * 删除前强制检查子分组与仓库（非空禁止删除）。
 */
@Service
@RequiredArgsConstructor
public class CodeRepositoryGroupService {

    private final CodeRepositoryGroupMapper groupMapper;
    private final CodeRepositoryMapper repositoryMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的分组列表（扁平列表，前端自行建树）
     * <p>repoCount 包含子分组的仓库数（自底向上聚合）。
     */
    public List<RepositoryGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<CodeRepositoryGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeRepositoryGroup::getProjectId, projectId);
        wrapper.orderByDesc(CodeRepositoryGroup::getIsSystem, CodeRepositoryGroup::getCreatedAt);

        List<CodeRepositoryGroup> list = groupMapper.selectList(wrapper);

        // 统计每个分组的直接仓库数
        Map<Long, Integer> directCountMap = new LinkedHashMap<>();
        for (CodeRepositoryGroup group : list) {
            LambdaQueryWrapper<CodeRepository> repoWrapper = new LambdaQueryWrapper<>();
            repoWrapper.eq(CodeRepository::getGroupId, group.getId());
            directCountMap.put(group.getId(), repositoryMapper.selectCount(repoWrapper).intValue());
        }

        // 建树后自底向上聚合子分组仓库数
        Map<Long, List<CodeRepositoryGroup>> childrenMap = list.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(CodeRepositoryGroup::getParentId));

        Map<Long, Integer> totalCountMap = new LinkedHashMap<>();
        for (CodeRepositoryGroup group : list) {
            totalCountMap.put(group.getId(), aggregateCount(group.getId(), directCountMap, childrenMap));
        }

        List<RepositoryGroupResponse> result = new ArrayList<>();
        for (CodeRepositoryGroup group : list) {
            RepositoryGroupResponse resp = toResponse(group);
            resp.setRepoCount(totalCountMap.getOrDefault(group.getId(), 0));
            result.add(resp);
        }
        return result;
    }

    /**
     * 查询项目下所有分组，返回以分组 ID 为 key 的 Map
     */
    public Map<Long, CodeRepositoryGroup> getGroupMap(Long projectId) {
        LambdaQueryWrapper<CodeRepositoryGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeRepositoryGroup::getProjectId, projectId);
        return groupMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(CodeRepositoryGroup::getId, g -> g, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 获取指定分组及其所有子孙分组的 ID 集合（用于仓库列表过滤）
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
    public RepositoryGroupResponse create(RepositoryGroupCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        if (request.getParentId() != null) {
            findById(request.getParentId());
        }
        checkNameDuplicate(request.getProjectId(), request.getName(), null);

        CodeRepositoryGroup group = new CodeRepositoryGroup();
        group.setProjectId(request.getProjectId());
        group.setParentId(request.getParentId());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setIsSystem(0);

        groupMapper.insert(group);
        return toResponse(group);
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public RepositoryGroupResponse update(Long groupId, RepositoryGroupUpdateRequest request) {
        CodeRepositoryGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.REPOSITORY_GROUP_SYSTEM, "系统分组不允许修改");
        }

        if (StringUtils.hasText(request.getName())) {
            checkNameDuplicate(group.getProjectId(), request.getName(), groupId);
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        // parentId：始终应用（null = 移到根级）
        Long newParentId = request.getParentId();
        if (newParentId != null && newParentId.equals(group.getId())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "不能将分组设为自身的子分组");
        }
        if (newParentId != null && getDescendantGroupIds(group.getId()).contains(newParentId)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "不能将分组移动到其子分组下");
        }
        group.setParentId(newParentId);

        groupMapper.updateById(group);
        return toResponse(group);
    }

    /**
     * 删除分组（系统分组不允许删除；存在子分组或仓库时禁止删除，需先移动内容）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        CodeRepositoryGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.REPOSITORY_GROUP_SYSTEM, "系统分组不允许删除");
        }

        // 检查是否有子分组
        LambdaQueryWrapper<CodeRepositoryGroup> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(CodeRepositoryGroup::getParentId, groupId);
        if (groupMapper.selectCount(childWrapper) > 0) {
            throw new BusinessException(ErrorCode.REPOSITORY_GROUP_NOT_EMPTY, "分组下存在子分组，请先删除子分组");
        }

        // 检查分组下是否有仓库
        LambdaQueryWrapper<CodeRepository> repoWrapper = new LambdaQueryWrapper<>();
        repoWrapper.eq(CodeRepository::getGroupId, groupId);
        if (repositoryMapper.selectCount(repoWrapper) > 0) {
            throw new BusinessException(ErrorCode.REPOSITORY_GROUP_NOT_EMPTY, "分组下存在仓库，请先移动仓库");
        }

        groupMapper.deleteById(groupId);
    }

    // ───────────────────── 私有方法 ─────────────────────

    /**
     * 递归聚合分组及其子分组的仓库数
     */
    private int aggregateCount(Long groupId, Map<Long, Integer> directCountMap,
                               Map<Long, List<CodeRepositoryGroup>> childrenMap) {
        int count = directCountMap.getOrDefault(groupId, 0);
        List<CodeRepositoryGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (CodeRepositoryGroup child : children) {
                count += aggregateCount(child.getId(), directCountMap, childrenMap);
            }
        }
        return count;
    }

    /**
     * 递归收集子孙分组 ID
     */
    private void collectDescendants(Long parentId, Set<Long> collected) {
        LambdaQueryWrapper<CodeRepositoryGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeRepositoryGroup::getParentId, parentId);
        List<CodeRepositoryGroup> children = groupMapper.selectList(wrapper);
        for (CodeRepositoryGroup child : children) {
            collected.add(child.getId());
            collectDescendants(child.getId(), collected);
        }
    }

    /**
     * 检查同项目下是否存在同名分组（排除指定 ID）
     */
    private void checkNameDuplicate(Long projectId, String name, Long excludeId) {
        LambdaQueryWrapper<CodeRepositoryGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeRepositoryGroup::getProjectId, projectId);
        wrapper.eq(CodeRepositoryGroup::getName, name);
        if (excludeId != null) {
            wrapper.ne(CodeRepositoryGroup::getId, excludeId);
        }
        if (groupMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.REPOSITORY_GROUP_NAME_DUPLICATE,
                    "分组名称已存在：" + name);
        }
    }

    private CodeRepositoryGroup findById(Long groupId) {
        CodeRepositoryGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.REPOSITORY_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }
        return group;
    }

    private RepositoryGroupResponse toResponse(CodeRepositoryGroup group) {
        RepositoryGroupResponse resp = new RepositoryGroupResponse();
        BeanUtils.copyProperties(group, resp);
        return resp;
    }
}
