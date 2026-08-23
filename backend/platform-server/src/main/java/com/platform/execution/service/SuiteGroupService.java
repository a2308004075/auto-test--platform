/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 套件分组管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.execution.dto.SuiteGroupDTO;
import com.platform.execution.dto.SuiteGroupRequest;
import com.platform.execution.entity.SuiteGroup;
import com.platform.execution.entity.TestSuite;
import com.platform.execution.mapper.SuiteGroupMapper;
import com.platform.execution.mapper.TestSuiteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 套件分组管理服务
 */
@Service
@RequiredArgsConstructor
public class SuiteGroupService {

    private final SuiteGroupMapper suiteGroupMapper;
    private final TestSuiteMapper testSuiteMapper;

    /**
     * 查询项目下所有分组（平铺列表，前端根据 parentId 构建树）
     *
     * <p>每个分组的 suiteCount 递归统计自身及所有后代分组下的套件总数。
     */
    public List<SuiteGroupDTO> listGroups(Long projectId) {
        LambdaQueryWrapper<SuiteGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SuiteGroup::getProjectId, projectId);
        wrapper.orderByAsc(SuiteGroup::getSortNo).orderByAsc(SuiteGroup::getCreatedAt);
        List<SuiteGroup> groups = suiteGroupMapper.selectList(wrapper);

        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建 parentId -> children 映射，用于递归计算套件数
        java.util.Map<Long, List<SuiteGroup>> childrenMap = new java.util.HashMap<>();
        for (SuiteGroup g : groups) {
            if (g.getParentId() != null) {
                childrenMap.computeIfAbsent(g.getParentId(), k -> new ArrayList<>()).add(g);
            }
        }

        List<SuiteGroupDTO> result = new ArrayList<>(groups.size());
        for (SuiteGroup g : groups) {
            SuiteGroupDTO dto = new SuiteGroupDTO();
            BeanUtils.copyProperties(g, dto);
            dto.setSuiteCount(countSuitesRecursive(g.getId(), childrenMap));
            result.add(dto);
        }
        return result;
    }

    /**
     * 递归统计分组下（含所有后代分组）的套件总数
     */
    private long countSuitesRecursive(Long groupId, java.util.Map<Long, List<SuiteGroup>> childrenMap) {
        LambdaQueryWrapper<TestSuite> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(TestSuite::getGroupId, groupId);
        long count = testSuiteMapper.selectCount(countWrapper);

        List<SuiteGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (SuiteGroup child : children) {
                count += countSuitesRecursive(child.getId(), childrenMap);
            }
        }
        return count;
    }

    /**
     * 创建分组
     */
    @Transactional(rollbackFor = Exception.class)
    public SuiteGroupDTO createGroup(Long projectId, SuiteGroupRequest request) {
        // 同层级下名称唯一性校验
        if (countByName(projectId, request.getParentId(), request.getName(), null) > 0) {
            throw new BusinessException(ErrorCode.SUITE_GROUP_NAME_DUPLICATE, "分组名称已存在：" + request.getName());
        }

        // 父分组存在性校验
        if (request.getParentId() != null) {
            SuiteGroup parent = suiteGroupMapper.selectById(request.getParentId());
            if (parent == null || !projectId.equals(parent.getProjectId())) {
                throw new BusinessException(ErrorCode.SUITE_GROUP_NOT_FOUND, "父分组不存在");
            }
        }

        SuiteGroup group = new SuiteGroup();
        BeanUtils.copyProperties(request, group);
        group.setProjectId(projectId);
        if (group.getSortNo() == null) {
            group.setSortNo(0);
        }
        suiteGroupMapper.insert(group);

        SuiteGroupDTO dto = new SuiteGroupDTO();
        BeanUtils.copyProperties(group, dto);
        dto.setSuiteCount(0L);
        return dto;
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public SuiteGroupDTO updateGroup(Long projectId, Long groupId, SuiteGroupRequest request) {
        SuiteGroup group = suiteGroupMapper.selectById(groupId);
        if (group == null || !projectId.equals(group.getProjectId())) {
            throw new BusinessException(ErrorCode.SUITE_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }

        // 同层级下名称唯一性校验
        Long newParentId = request.getParentId() != null ? request.getParentId() : group.getParentId();
        if (!request.getName().equals(group.getName()) || !java.util.Objects.equals(newParentId, group.getParentId())) {
            if (countByName(projectId, newParentId, request.getName(), groupId) > 0) {
                throw new BusinessException(ErrorCode.SUITE_GROUP_NAME_DUPLICATE, "分组名称已存在：" + request.getName());
            }
        }

        // 防止将自身设为父分组
        if (request.getParentId() != null && request.getParentId().equals(groupId)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "不能将分组设为自身的子分组");
        }

        group.setName(request.getName());
        group.setParentId(request.getParentId());
        if (request.getSortNo() != null) {
            group.setSortNo(request.getSortNo());
        }
        suiteGroupMapper.updateById(group);

        SuiteGroupDTO dto = new SuiteGroupDTO();
        BeanUtils.copyProperties(group, dto);
        // 构建 childrenMap 用于递归统计
        java.util.Map<Long, List<SuiteGroup>> childrenMap = new java.util.HashMap<>();
        LambdaQueryWrapper<SuiteGroup> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(SuiteGroup::getProjectId, projectId);
        for (SuiteGroup g : suiteGroupMapper.selectList(allWrapper)) {
            if (g.getParentId() != null) {
                childrenMap.computeIfAbsent(g.getParentId(), k -> new ArrayList<>()).add(g);
            }
        }
        dto.setSuiteCount(countSuitesRecursive(group.getId(), childrenMap));
        return dto;
    }

    /**
     * 删除分组（该分组下的套件自动归入未分组）
     *
     * <p>如果分组下有子分组，先将子分组下的套件归入未分组，再删除子分组，最后删除本分组。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long projectId, Long groupId) {
        SuiteGroup group = suiteGroupMapper.selectById(groupId);
        if (group == null || !projectId.equals(group.getProjectId())) {
            throw new BusinessException(ErrorCode.SUITE_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }

        // 递归收集所有后代分组 ID
        List<Long> allIds = collectDescendantIds(groupId);
        allIds.add(groupId);

        // 将所有后代分组和本分组下的套件归入未分组（group_id = null）
        for (Long id : allIds) {
            LambdaQueryWrapper<TestSuite> suiteWrapper = new LambdaQueryWrapper<>();
            suiteWrapper.eq(TestSuite::getGroupId, id);
            List<TestSuite> suites = testSuiteMapper.selectList(suiteWrapper);
            for (TestSuite s : suites) {
                s.setGroupId(null);
                testSuiteMapper.updateById(s);
            }
        }

        // 从叶子到根依次删除分组
        for (int i = allIds.size() - 1; i >= 0; i--) {
            suiteGroupMapper.deleteById(allIds.get(i));
        }
    }

    /**
     * 递归收集所有后代分组 ID
     */
    private List<Long> collectDescendantIds(Long parentId) {
        LambdaQueryWrapper<SuiteGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SuiteGroup::getParentId, parentId);
        List<SuiteGroup> children = suiteGroupMapper.selectList(wrapper);

        List<Long> ids = new ArrayList<>();
        for (SuiteGroup child : children) {
            ids.add(child.getId());
            ids.addAll(collectDescendantIds(child.getId()));
        }
        return ids;
    }

    /**
     * 同层级名称唯一性校验
     */
    private long countByName(Long projectId, Long parentId, String name, Long excludeId) {
        LambdaQueryWrapper<SuiteGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SuiteGroup::getProjectId, projectId);
        if (parentId != null) {
            wrapper.eq(SuiteGroup::getParentId, parentId);
        } else {
            wrapper.isNull(SuiteGroup::getParentId);
        }
        wrapper.eq(SuiteGroup::getName, name);
        if (excludeId != null) {
            wrapper.ne(SuiteGroup::getId, excludeId);
        }
        return suiteGroupMapper.selectCount(wrapper);
    }
}
