/**
 * @author HXN
 * @date 2026-08-26
 * @description 接口关键字分组管理服务
 */
package com.platform.keyword.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.auth.entity.User;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.keyword.dto.ApiKeywordGroupCreateRequest;
import com.platform.keyword.dto.ApiKeywordGroupResponse;
import com.platform.keyword.dto.ApiKeywordGroupUpdateRequest;
import com.platform.keyword.entity.ApiKeyword;
import com.platform.keyword.entity.ApiKeywordGroup;
import com.platform.keyword.mapper.ApiKeywordGroupMapper;
import com.platform.keyword.mapper.ApiKeywordMapper;
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
 * 接口关键字分组管理服务
 */
@Service
@RequiredArgsConstructor
public class ApiKeywordGroupService {

    private final ApiKeywordGroupMapper apiKeywordGroupMapper;
    private final ApiKeywordMapper apiKeywordMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的分组列表（扁平列表，前端自行建树）
     * <p>keywordCount 包含子分组的关键字数（自底向上聚合）。
     */
    public List<ApiKeywordGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<ApiKeywordGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeywordGroup::getProjectId, projectId);
        wrapper.orderByDesc(ApiKeywordGroup::getIsSystem, ApiKeywordGroup::getCreatedAt);

        List<ApiKeywordGroup> list = apiKeywordGroupMapper.selectList(wrapper);

        // 统计每个分组的直接关键字数
        Map<Long, Integer> directCountMap = new LinkedHashMap<>();
        for (ApiKeywordGroup group : list) {
            directCountMap.put(group.getId(), countKeywords(group.getId()));
        }

        // 建树后自底向上聚合子分组关键字数
        Map<Long, List<ApiKeywordGroup>> childrenMap = list.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(ApiKeywordGroup::getParentId));

        Map<Long, Integer> totalCountMap = new LinkedHashMap<>();
        for (ApiKeywordGroup group : list) {
            totalCountMap.put(group.getId(), aggregateCount(group.getId(), directCountMap, childrenMap));
        }

        List<ApiKeywordGroupResponse> result = new ArrayList<>();
        for (ApiKeywordGroup group : list) {
            ApiKeywordGroupResponse resp = toResponse(group);
            resp.setKeywordCount(totalCountMap.getOrDefault(group.getId(), 0));
            result.add(resp);
        }
        return result;
    }

    /**
     * 获取指定分组及其所有子孙分组的 ID 集合（用于关键字列表过滤）
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
    public ApiKeywordGroupResponse create(ApiKeywordGroupCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        ApiKeywordGroup group = new ApiKeywordGroup();
        group.setProjectId(request.getProjectId());
        group.setParentId(request.getParentId());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setIsSystem(0);
        group.setCreatedBy(getCurrentUserId());

        apiKeywordGroupMapper.insert(group);
        ApiKeywordGroupResponse resp = toResponse(group);
        resp.setKeywordCount(0);
        return resp;
    }

    /**
     * 更新分组
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiKeywordGroupResponse update(Long groupId, ApiKeywordGroupUpdateRequest request) {
        ApiKeywordGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.KEYWORD_GROUP_SYSTEM, "系统分组不允许修改");
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

        apiKeywordGroupMapper.updateById(group);

        ApiKeywordGroupResponse resp = toResponse(group);
        resp.setKeywordCount(countKeywords(group.getId()));
        return resp;
    }

    /**
     * 删除分组（系统分组不允许删除）
     * <p>删除时将该分组及其子孙分组下的关键字 groupId 设为 NULL（归入未分组）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        ApiKeywordGroup group = findById(groupId);

        if (Integer.valueOf(1).equals(group.getIsSystem())) {
            throw new BusinessException(ErrorCode.KEYWORD_GROUP_SYSTEM, "系统分组不允许删除");
        }

        // 收集该分组及所有子孙分组 ID
        Set<Long> descendantIds = getDescendantGroupIds(groupId);

        // 将这些分组下的关键字 groupId 设为 NULL
        for (Long gid : descendantIds) {
            LambdaQueryWrapper<ApiKeyword> keywordWrapper = new LambdaQueryWrapper<>();
            keywordWrapper.eq(ApiKeyword::getGroupId, gid);
            List<ApiKeyword> keywords = apiKeywordMapper.selectList(keywordWrapper);
            for (ApiKeyword kw : keywords) {
                kw.setGroupId(null);
                apiKeywordMapper.updateById(kw);
            }
        }

        // 删除子孙分组（从叶子到根）
        for (Long gid : descendantIds) {
            if (!gid.equals(groupId)) {
                apiKeywordGroupMapper.deleteById(gid);
            }
        }
        apiKeywordGroupMapper.deleteById(groupId);
    }

    // ───────────────────── 私有方法 ─────────────────────

    /**
     * 递归聚合分组及其子分组的关键字数
     */
    private int aggregateCount(Long groupId, Map<Long, Integer> directCountMap,
                               Map<Long, List<ApiKeywordGroup>> childrenMap) {
        int count = directCountMap.getOrDefault(groupId, 0);
        List<ApiKeywordGroup> children = childrenMap.get(groupId);
        if (children != null) {
            for (ApiKeywordGroup child : children) {
                count += aggregateCount(child.getId(), directCountMap, childrenMap);
            }
        }
        return count;
    }

    /**
     * 递归收集子孙分组 ID
     */
    private void collectDescendants(Long parentId, Set<Long> collected) {
        LambdaQueryWrapper<ApiKeywordGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeywordGroup::getParentId, parentId);
        List<ApiKeywordGroup> children = apiKeywordGroupMapper.selectList(wrapper);
        for (ApiKeywordGroup child : children) {
            collected.add(child.getId());
            collectDescendants(child.getId(), collected);
        }
    }

    private ApiKeywordGroup findById(Long groupId) {
        ApiKeywordGroup group = apiKeywordGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.KEYWORD_GROUP_NOT_FOUND, "接口关键字分组不存在");
        }
        return group;
    }

    private int countKeywords(Long groupId) {
        LambdaQueryWrapper<ApiKeyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyword::getGroupId, groupId);
        return apiKeywordMapper.selectCount(wrapper).intValue();
    }

    private ApiKeywordGroupResponse toResponse(ApiKeywordGroup group) {
        ApiKeywordGroupResponse resp = new ApiKeywordGroupResponse();
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
