/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 模块管理服务
 */
package com.platform.apidoc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.apidoc.dto.ApiModuleCreateRequest;
import com.platform.apidoc.dto.ApiModuleResponse;
import com.platform.apidoc.dto.ApiModuleUpdateRequest;
import com.platform.apidoc.entity.Api;
import com.platform.apidoc.mapper.ApiMapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.project.entity.ApiModule;
import com.platform.project.mapper.ApiModuleMapper;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 接口分组管理服务
 */
@Service
@RequiredArgsConstructor
public class ApiModuleService {

    private final ApiModuleMapper apiModuleMapper;
    private final ApiMapper apiMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的分组列表（扁平列表，前端自行建树）
     * <p>apiCount 包含子分组的接口数（自底向上聚合）。
     */
    public List<ApiModuleResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<ApiModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiModule::getProjectId, projectId);
        wrapper.orderByDesc(ApiModule::getIsSystem, ApiModule::getCreatedAt);

        List<ApiModule> list = apiModuleMapper.selectList(wrapper);

        // 统计每个分组的直接接口数
        Map<Long, Integer> directCountMap = new LinkedHashMap<>();
        for (ApiModule module : list) {
            LambdaQueryWrapper<Api> apiWrapper = new LambdaQueryWrapper<>();
            apiWrapper.eq(Api::getModuleId, module.getId());
            directCountMap.put(module.getId(), apiMapper.selectCount(apiWrapper).intValue());
        }

        // 建树后自底向上聚合子分组接口数
        Map<Long, List<ApiModule>> childrenMap = list.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(ApiModule::getParentId));

        Map<Long, Integer> totalCountMap = new LinkedHashMap<>();
        for (ApiModule module : list) {
            totalCountMap.put(module.getId(), aggregateCount(module.getId(), directCountMap, childrenMap));
        }

        List<ApiModuleResponse> result = new ArrayList<>();
        for (ApiModule module : list) {
            ApiModuleResponse resp = toResponse(module);
            resp.setApiCount(totalCountMap.getOrDefault(module.getId(), 0));
            result.add(resp);
        }
        return result;
    }

    /**
     * 获取指定分组及其所有子孙分组的 ID 集合（用于接口列表过滤）
     */
    public Set<Long> getDescendantModuleIds(Long moduleId) {
        Set<Long> result = new LinkedHashSet<>();
        result.add(moduleId);
        collectDescendants(moduleId, result);
        return result;
    }

    /**
     * 创建分组
     */
    public ApiModuleResponse create(ApiModuleCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        ApiModule module = new ApiModule();
        module.setProjectId(request.getProjectId());
        module.setParentId(request.getParentId());
        module.setName(request.getName());
        module.setServicePrefix(request.getServicePrefix());
        module.setDescription(request.getDescription());
        module.setSourceType("MANUAL");
        module.setIsSystem(0);

        apiModuleMapper.insert(module);
        return toResponse(module);
    }

    /**
     * 更新分组
     */
    public ApiModuleResponse update(Long moduleId, ApiModuleUpdateRequest request) {
        ApiModule module = findById(moduleId);

        if (Integer.valueOf(1).equals(module.getIsSystem())) {
            throw new BusinessException(ErrorCode.API_MODULE_SYSTEM, "系统分组不允许修改");
        }

        if (StringUtils.hasText(request.getName())) {
            module.setName(request.getName());
        }
        if (request.getServicePrefix() != null) {
            module.setServicePrefix(request.getServicePrefix());
        }
        if (request.getDescription() != null) {
            module.setDescription(request.getDescription());
        }
        if (request.getParentId() != null) {
            module.setParentId(request.getParentId());
        }

        apiModuleMapper.updateById(module);
        return toResponse(module);
    }

    /**
     * 删除分组（系统分组不允许删除）
     */
    public void delete(Long moduleId) {
        ApiModule module = findById(moduleId);

        if (Integer.valueOf(1).equals(module.getIsSystem())) {
            throw new BusinessException(ErrorCode.API_MODULE_SYSTEM, "系统分组不允许删除");
        }

        // 检查是否有子分组
        LambdaQueryWrapper<ApiModule> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(ApiModule::getParentId, moduleId);
        if (apiModuleMapper.selectCount(childWrapper) > 0) {
            throw new BusinessException(ErrorCode.API_MODULE_HAS_APIS, "分组下存在子分组，请先删除子分组");
        }

        // 检查分组下是否有接口
        LambdaQueryWrapper<Api> apiWrapper = new LambdaQueryWrapper<>();
        apiWrapper.eq(Api::getModuleId, moduleId);
        if (apiMapper.selectCount(apiWrapper) > 0) {
            throw new BusinessException(ErrorCode.API_MODULE_HAS_APIS, "分组下存在接口，请先删除接口");
        }

        apiModuleMapper.deleteById(moduleId);
    }

    /**
     * 获取分组详情
     */
    public ApiModuleResponse getById(Long moduleId) {
        ApiModule module = findById(moduleId);
        ApiModuleResponse resp = toResponse(module);
        LambdaQueryWrapper<Api> apiWrapper = new LambdaQueryWrapper<>();
        apiWrapper.eq(Api::getModuleId, moduleId);
        resp.setApiCount(apiMapper.selectCount(apiWrapper).intValue());
        return resp;
    }

    // ───────────────────── 私有方法 ─────────────────────

    /**
     * 递归聚合分组及其子分组的接口数
     */
    private int aggregateCount(Long moduleId, Map<Long, Integer> directCountMap,
                                Map<Long, List<ApiModule>> childrenMap) {
        int count = directCountMap.getOrDefault(moduleId, 0);
        List<ApiModule> children = childrenMap.get(moduleId);
        if (children != null) {
            for (ApiModule child : children) {
                count += aggregateCount(child.getId(), directCountMap, childrenMap);
            }
        }
        return count;
    }

    /**
     * 递归收集子孙分组 ID
     */
    private void collectDescendants(Long parentId, Set<Long> collected) {
        LambdaQueryWrapper<ApiModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiModule::getParentId, parentId);
        List<ApiModule> children = apiModuleMapper.selectList(wrapper);
        for (ApiModule child : children) {
            collected.add(child.getId());
            collectDescendants(child.getId(), collected);
        }
    }

    private ApiModule findById(Long moduleId) {
        ApiModule module = apiModuleMapper.selectById(moduleId);
        if (module == null) {
            throw new BusinessException(ErrorCode.API_MODULE_NOT_FOUND, "分组不存在：" + moduleId);
        }
        return module;
    }

    private ApiModuleResponse toResponse(ApiModule module) {
        ApiModuleResponse resp = new ApiModuleResponse();
        BeanUtils.copyProperties(module, resp);
        return resp;
    }
}
