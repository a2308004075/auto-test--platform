package com.postman.platform.apidoc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.postman.platform.apidoc.dto.ApiModuleCreateRequest;
import com.postman.platform.apidoc.dto.ApiModuleResponse;
import com.postman.platform.apidoc.dto.ApiModuleUpdateRequest;
import com.postman.platform.apidoc.entity.Api;
import com.postman.platform.apidoc.mapper.ApiMapper;
import com.postman.platform.common.exception.BusinessException;
import com.postman.platform.common.exception.ErrorCode;
import com.postman.platform.project.entity.ApiModule;
import com.postman.platform.project.mapper.ApiModuleMapper;
import com.postman.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
     * 查询项目下的分组列表
     */
    public List<ApiModuleResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<ApiModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiModule::getProjectId, projectId);
        wrapper.orderByDesc(ApiModule::getIsSystem, ApiModule::getCreatedAt);

        List<ApiModule> list = apiModuleMapper.selectList(wrapper);
        List<ApiModuleResponse> result = new ArrayList<>();
        for (ApiModule module : list) {
            ApiModuleResponse resp = toResponse(module);
            // 统计分组下的接口数
            LambdaQueryWrapper<Api> apiWrapper = new LambdaQueryWrapper<>();
            apiWrapper.eq(Api::getModuleId, module.getId());
            resp.setApiCount(apiMapper.selectCount(apiWrapper).intValue());
            result.add(resp);
        }
        return result;
    }

    /**
     * 创建分组
     */
    public ApiModuleResponse create(ApiModuleCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        ApiModule module = new ApiModule();
        module.setProjectId(request.getProjectId());
        module.setName(request.getName());
        module.setServicePrefix(request.getServicePrefix());
        module.setDescription(request.getDescription());
        module.setSourceType("MANUAL");
        module.setIsSystem(false);

        apiModuleMapper.insert(module);
        return toResponse(module);
    }

    /**
     * 更新分组
     */
    public ApiModuleResponse update(Long moduleId, ApiModuleUpdateRequest request) {
        ApiModule module = findById(moduleId);

        if (Boolean.TRUE.equals(module.getIsSystem())) {
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

        apiModuleMapper.updateById(module);
        return toResponse(module);
    }

    /**
     * 删除分组（系统分组不允许删除）
     */
    public void delete(Long moduleId) {
        ApiModule module = findById(moduleId);

        if (Boolean.TRUE.equals(module.getIsSystem())) {
            throw new BusinessException(ErrorCode.API_MODULE_SYSTEM, "系统分组不允许删除");
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
