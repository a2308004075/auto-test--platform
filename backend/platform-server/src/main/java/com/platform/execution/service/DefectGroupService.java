/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷分组管理服务
 */
package com.platform.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.execution.dto.DefectGroupCreateRequest;
import com.platform.execution.dto.DefectGroupResponse;
import com.platform.execution.dto.DefectGroupUpdateRequest;
import com.platform.execution.entity.Defect;
import com.platform.execution.entity.DefectGroup;
import com.platform.execution.mapper.DefectGroupMapper;
import com.platform.execution.mapper.DefectMapper;
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
 * 缺陷分组管理服务
 */
@Service
@RequiredArgsConstructor
public class DefectGroupService {

    private final DefectGroupMapper defectGroupMapper;
    private final DefectMapper defectMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的分组列表（扁平列表，前端自行建树）
     */
    public List<DefectGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<DefectGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DefectGroup::getProjectId, projectId);
        wrapper.orderByDesc(DefectGroup::getIsSystem, DefectGroup::getCreatedAt);

        List<DefectGroup> list = defectGroupMapper.selectList(wrapper);

        Map<Long, Integer> directCountMap = new LinkedHashMap<>();
        for (DefectGroup group : list) {
            LambdaQueryWrapper<Defect> defectWrapper = new LambdaQueryWrapper<>();
            defectWrapper.eq(Defect::getGroupId, group.getId());
            directCountMap.put(group.getId(), defectMapper.selectCount(defectWrapper).intValue());
        }

        Map<Long, List<DefectGroup>> childrenMap = list.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(DefectGroup::getParentId));

        Map<Long, Integer> totalCountMap = new LinkedHashMap<>();
        for (DefectGroup group : list) {
            totalCountMap.put(group.getId(), aggregateCount(group.getId(), directCountMap, childrenMap));
        }

        List<DefectGroupResponse> result = new ArrayList<>();
        for (DefectGroup group : list) {
            DefectGroupResponse resp = toResponse(group);
            resp.setDefectCount(totalCountMap.getOrDefault(group.getId(), 0));
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
    public DefectGroupResponse create(DefectGroupCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        DefectGroup group = new DefectGroup();
        group.setProjectId(request.getProjectId());
        group.setParentId(request.getParentId());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setIsSystem(0);
        group.setCreatedBy(getCurrentUserId());

        defectGroupMapper.insert(group);
        return toResponse(group);
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public DefectGroupResponse update(Long groupId, DefectGroupUpdateRequest request) {
        DefectGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.DEFECT_GROUP_SYSTEM, "系统分组不允许修改");
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

        defectGroupMapper.updateById(group);
        return toResponse(group);
    }

    /**
     * 删除分组（系统分组不允许删除）
     * <p>删除时将该分组及其子分组下的缺陷 groupId 设为 NULL（归入未分组）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        DefectGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.DEFECT_GROUP_SYSTEM, "系统分组不允许删除");
        }

        Set<Long> descendantIds = getDescendantGroupIds(groupId);

        for (Long gid : descendantIds) {
            LambdaQueryWrapper<Defect> defectWrapper = new LambdaQueryWrapper<>();
            defectWrapper.eq(Defect::getGroupId, gid);
            List<Defect> defects = defectMapper.selectList(defectWrapper);
            for (Defect d : defects) {
                d.setGroupId(null);
                defectMapper.updateById(d);
            }
        }

        for (Long gid : descendantIds) {
            if (!gid.equals(groupId)) {
                defectGroupMapper.deleteById(gid);
            }
        }
        defectGroupMapper.deleteById(groupId);
    }

    // ───────────────────── 私有方法 ─────────────────────

    private int aggregateCount(Long groupId, Map<Long, Integer> directCountMap,
                               Map<Long, List<DefectGroup>> childrenMap) {
        int count = directCountMap.getOrDefault(groupId, 0);
        List<DefectGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (DefectGroup child : children) {
                count += aggregateCount(child.getId(), directCountMap, childrenMap);
            }
        }
        return count;
    }

    private void collectDescendants(Long parentId, Set<Long> collected) {
        LambdaQueryWrapper<DefectGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DefectGroup::getParentId, parentId);
        List<DefectGroup> children = defectGroupMapper.selectList(wrapper);
        for (DefectGroup child : children) {
            collected.add(child.getId());
            collectDescendants(child.getId(), collected);
        }
    }

    DefectGroup findById(Long groupId) {
        DefectGroup group = defectGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.DEFECT_GROUP_NOT_FOUND, "分组不存在：" + groupId);
        }
        return group;
    }

    private DefectGroupResponse toResponse(DefectGroup group) {
        DefectGroupResponse resp = new DefectGroupResponse();
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
