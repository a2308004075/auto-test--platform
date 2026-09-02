/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 自动化套件分组管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.execution.dto.AutoSuiteGroupDTO;
import com.platform.execution.dto.AutoSuiteGroupRequest;
import com.platform.execution.entity.AutoSuite;
import com.platform.execution.entity.AutoSuiteGroup;
import com.platform.execution.mapper.AutoSuiteGroupMapper;
import com.platform.execution.mapper.AutoSuiteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自动化套件分组管理服务
 */
@Service
@RequiredArgsConstructor
public class AutoSuiteGroupService {

    private final AutoSuiteGroupMapper autoSuiteGroupMapper;
    private final AutoSuiteMapper autoSuiteMapper;

    /**
     * 查询项目下所有分组（平铺列表，前端根据 parentId 构建树）
     *
     * <p>每个分组的 suiteCount 递归统计自身及所有后代分组下的自动化套件总数。
     */
    public List<AutoSuiteGroupDTO> listGroups(Long projectId) {
        LambdaQueryWrapper<AutoSuiteGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuiteGroup::getProjectId, projectId);
        wrapper.orderByAsc(AutoSuiteGroup::getSortNo).orderByAsc(AutoSuiteGroup::getCreatedAt);
        List<AutoSuiteGroup> groups = autoSuiteGroupMapper.selectList(wrapper);

        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建 parentId -> children 映射，用于递归计算自动化套件数
        java.util.Map<Long, List<AutoSuiteGroup>> childrenMap = new java.util.HashMap<>();
        for (AutoSuiteGroup g : groups) {
            if (g.getParentId() != null) {
                childrenMap.computeIfAbsent(g.getParentId(), k -> new ArrayList<>()).add(g);
            }
        }

        List<AutoSuiteGroupDTO> result = new ArrayList<>(groups.size());
        for (AutoSuiteGroup g : groups) {
            AutoSuiteGroupDTO dto = new AutoSuiteGroupDTO();
            BeanUtils.copyProperties(g, dto);
            dto.setSuiteCount(countSuitesRecursive(g.getId(), childrenMap));
            result.add(dto);
        }
        return result;
    }

    /**
     * 递归统计分组下（含所有后代分组）的自动化套件总数
     */
    private long countSuitesRecursive(Long groupId, java.util.Map<Long, List<AutoSuiteGroup>> childrenMap) {
        LambdaQueryWrapper<AutoSuite> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(AutoSuite::getGroupId, groupId);
        long count = autoSuiteMapper.selectCount(countWrapper);

        List<AutoSuiteGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (AutoSuiteGroup child : children) {
                count += countSuitesRecursive(child.getId(), childrenMap);
            }
        }
        return count;
    }

    /**
     * 创建分组
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoSuiteGroupDTO createGroup(Long projectId, AutoSuiteGroupRequest request) {
        // 同层级下名称唯一性校验
        if (countByName(projectId, request.getParentId(), request.getName(), null) > 0) {
            throw new BusinessException(ErrorCode.AUTO_SUITE_GROUP_NAME_DUPLICATE, "分组名称已存在：" + request.getName());
        }

        // 父分组存在性校验
        if (request.getParentId() != null) {
            AutoSuiteGroup parent = autoSuiteGroupMapper.selectById(request.getParentId());
            if (parent == null || !projectId.equals(parent.getProjectId())) {
                throw new BusinessException(ErrorCode.AUTO_SUITE_GROUP_NOT_FOUND, "父分组不存在");
            }
        }

        AutoSuiteGroup group = new AutoSuiteGroup();
        BeanUtils.copyProperties(request, group);
        group.setProjectId(projectId);
        if (group.getSortNo() == null) {
            group.setSortNo(0);
        }
        autoSuiteGroupMapper.insert(group);

        AutoSuiteGroupDTO dto = new AutoSuiteGroupDTO();
        BeanUtils.copyProperties(group, dto);
        dto.setSuiteCount(0L);
        return dto;
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoSuiteGroupDTO updateGroup(Long projectId, Long groupId, AutoSuiteGroupRequest request) {
        AutoSuiteGroup group = autoSuiteGroupMapper.selectById(groupId);
        if (group == null || !projectId.equals(group.getProjectId())) {
            throw new BusinessException(ErrorCode.AUTO_SUITE_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }

        // 同层级下名称唯一性校验
        Long newParentId = request.getParentId() != null ? request.getParentId() : group.getParentId();
        if (!request.getName().equals(group.getName()) || !java.util.Objects.equals(newParentId, group.getParentId())) {
            if (countByName(projectId, newParentId, request.getName(), groupId) > 0) {
                throw new BusinessException(ErrorCode.AUTO_SUITE_GROUP_NAME_DUPLICATE, "分组名称已存在：" + request.getName());
            }
        }

        // 防止将自身设为父分组
        if (request.getParentId() != null && request.getParentId().equals(groupId)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "不能将分组设为自身的子分组");
        }

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setParentId(request.getParentId());
        if (request.getSortNo() != null) {
            group.setSortNo(request.getSortNo());
        }
        autoSuiteGroupMapper.updateById(group);

        AutoSuiteGroupDTO dto = new AutoSuiteGroupDTO();
        BeanUtils.copyProperties(group, dto);
        // 构建 childrenMap 用于递归统计
        java.util.Map<Long, List<AutoSuiteGroup>> childrenMap = new java.util.HashMap<>();
        LambdaQueryWrapper<AutoSuiteGroup> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(AutoSuiteGroup::getProjectId, projectId);
        for (AutoSuiteGroup g : autoSuiteGroupMapper.selectList(allWrapper)) {
            if (g.getParentId() != null) {
                childrenMap.computeIfAbsent(g.getParentId(), k -> new ArrayList<>()).add(g);
            }
        }
        dto.setSuiteCount(countSuitesRecursive(group.getId(), childrenMap));
        return dto;
    }

    /**
     * 删除分组（该分组下的自动化套件自动归入未分组）
     *
     * <p>如果分组下有子分组，先将子分组下的自动化套件归入未分组，再删除子分组，最后删除本分组。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long projectId, Long groupId) {
        AutoSuiteGroup group = autoSuiteGroupMapper.selectById(groupId);
        if (group == null || !projectId.equals(group.getProjectId())) {
            throw new BusinessException(ErrorCode.AUTO_SUITE_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }

        // 递归收集所有后代分组 ID
        List<Long> allIds = collectDescendantIds(groupId);
        allIds.add(groupId);

        // 将所有后代分组和本分组下的自动化套件归入未分组（group_id = null）
        for (Long id : allIds) {
            LambdaQueryWrapper<AutoSuite> suiteWrapper = new LambdaQueryWrapper<>();
            suiteWrapper.eq(AutoSuite::getGroupId, id);
            List<AutoSuite> suites = autoSuiteMapper.selectList(suiteWrapper);
            for (AutoSuite s : suites) {
                s.setGroupId(null);
                autoSuiteMapper.updateById(s);
            }
        }

        // 从叶子到根依次删除分组
        for (int i = allIds.size() - 1; i >= 0; i--) {
            autoSuiteGroupMapper.deleteById(allIds.get(i));
        }
    }

    /**
     * 递归收集所有后代分组 ID
     */
    private List<Long> collectDescendantIds(Long parentId) {
        LambdaQueryWrapper<AutoSuiteGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuiteGroup::getParentId, parentId);
        List<AutoSuiteGroup> children = autoSuiteGroupMapper.selectList(wrapper);

        List<Long> ids = new ArrayList<>();
        for (AutoSuiteGroup child : children) {
            ids.add(child.getId());
            ids.addAll(collectDescendantIds(child.getId()));
        }
        return ids;
    }

    /**
     * 同层级名称唯一性校验
     */
    private long countByName(Long projectId, Long parentId, String name, Long excludeId) {
        LambdaQueryWrapper<AutoSuiteGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutoSuiteGroup::getProjectId, projectId);
        if (parentId != null) {
            wrapper.eq(AutoSuiteGroup::getParentId, parentId);
        } else {
            wrapper.isNull(AutoSuiteGroup::getParentId);
        }
        wrapper.eq(AutoSuiteGroup::getName, name);
        if (excludeId != null) {
            wrapper.ne(AutoSuiteGroup::getId, excludeId);
        }
        return autoSuiteGroupMapper.selectCount(wrapper);
    }
}
