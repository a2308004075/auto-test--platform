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

import java.util.ArrayList;
import java.util.List;

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
     * 查询项目下的接口关键字分组列表
     */
    public List<ApiKeywordGroupResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<ApiKeywordGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeywordGroup::getProjectId, projectId);
        wrapper.orderByAsc(ApiKeywordGroup::getCreatedAt);

        List<ApiKeywordGroup> list = apiKeywordGroupMapper.selectList(wrapper);
        List<ApiKeywordGroupResponse> result = new ArrayList<>();
        for (ApiKeywordGroup group : list) {
            ApiKeywordGroupResponse resp = toResponse(group);
            resp.setKeywordCount(countKeywords(group.getId()));
            result.add(resp);
        }
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
        group.setName(request.getName());
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

        if (StringUtils.hasText(request.getName())) {
            group.setName(request.getName());
        }
        apiKeywordGroupMapper.updateById(group);

        ApiKeywordGroupResponse resp = toResponse(group);
        resp.setKeywordCount(countKeywords(group.getId()));
        return resp;
    }

    /**
     * 删除分组
     * <p>删除前将该分组下的接口关键字 group_id 置为 NULL。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        ApiKeywordGroup group = findById(groupId);

        LambdaQueryWrapper<ApiKeyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyword::getGroupId, group.getId());
        List<ApiKeyword> keywords = apiKeywordMapper.selectList(wrapper);
        for (ApiKeyword kw : keywords) {
            kw.setGroupId(null);
            apiKeywordMapper.updateById(kw);
        }

        apiKeywordGroupMapper.deleteById(group.getId());
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
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        return null;
    }
}
