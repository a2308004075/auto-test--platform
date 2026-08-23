/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 环境管理服务
 */
package com.platform.environment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.environment.dto.*;
import com.platform.environment.entity.Environment;
import com.platform.environment.entity.EnvironmentVariable;
import com.platform.environment.mapper.EnvironmentMapper;
import com.platform.environment.mapper.EnvironmentVariableMapper;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 环境配置管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentService {

    private final EnvironmentMapper environmentMapper;
    private final EnvironmentVariableMapper environmentVariableMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的环境列表（基本信息，不含变量详情）
     */
    public List<EnvironmentResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<Environment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Environment::getProjectId, projectId);
        wrapper.orderByDesc(Environment::getIsCurrent, Environment::getCreatedAt);

        List<Environment> list = environmentMapper.selectList(wrapper);
        List<EnvironmentResponse> result = new ArrayList<>();
        for (Environment env : list) {
            result.add(toBasicResponse(env));
        }
        return result;
    }

    /**
     * 创建环境（仅基本信息，变量通过编辑页单独管理）
     */
    public EnvironmentResponse create(EnvironmentCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        Environment env = new Environment();
        env.setProjectId(request.getProjectId());
        env.setName(request.getName());
        env.setDescription(request.getDescription());
        env.setIsCurrent(0);

        environmentMapper.insert(env);
        return toBasicResponse(env);
    }

    /**
     * 更新环境（基本信息 + 全量替换变量）
     */
    @Transactional(rollbackFor = Exception.class)
    public EnvironmentResponse update(Long envId, EnvironmentUpdateRequest request) {
        Environment env = findById(envId);

        if (StringUtils.hasText(request.getName())) {
            env.setName(request.getName());
        }
        if (request.getDescription() != null) {
            env.setDescription(request.getDescription());
        }
        environmentMapper.updateById(env);

        // 全量替换变量（先删后插）
        if (request.getVariables() != null) {
            replaceVariables(envId, request.getVariables());
        }

        return getDetail(envId);
    }

    /**
     * 删除环境（变量由 FK ON DELETE CASCADE 级联删除）
     */
    public void delete(Long envId) {
        findById(envId);
        environmentMapper.deleteById(envId);
    }

    /**
     * 激活环境（互斥：同一项目下只能有一个激活环境）
     */
    @Transactional(rollbackFor = Exception.class)
    public EnvironmentResponse activate(Long envId) {
        Environment env = findById(envId);

        if (Integer.valueOf(1).equals(env.getIsCurrent())) {
            // 取消激活
            env.setIsCurrent(0);
            environmentMapper.updateById(env);
        } else {
            // 先将同项目下其他环境取消激活
            LambdaUpdateWrapper<Environment> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Environment::getProjectId, env.getProjectId())
                    .eq(Environment::getIsCurrent, 1)
                    .set(Environment::getIsCurrent, 0);
            environmentMapper.update(null, updateWrapper);

            // 激活目标环境
            env.setIsCurrent(1);
            environmentMapper.updateById(env);
        }

        return toBasicResponse(env);
    }

    /**
     * 获取环境详情（含变量列表）
     */
    public EnvironmentResponse getDetail(Long envId) {
        Environment env = findById(envId);
        EnvironmentResponse response = toBasicResponse(env);
        response.setVariables(listVariables(envId));
        return response;
    }

    /**
     * 获取当前激活的环境
     */
    public EnvironmentResponse getActiveEnvironment(Long projectId) {
        LambdaQueryWrapper<Environment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Environment::getProjectId, projectId)
                .eq(Environment::getIsCurrent, 1)
                .last("LIMIT 1");
        Environment env = environmentMapper.selectOne(wrapper);
        if (env == null) {
            return null;
        }
        return toBasicResponse(env);
    }

    /**
     * 获取环境变量 Map（供执行引擎和调试服务使用）
     *
     * @param envId 环境 ID
     * @return 变量名 → 变量值 的 Map
     */
    public Map<String, String> getVariablesAsMap(Long envId) {
        LambdaQueryWrapper<EnvironmentVariable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnvironmentVariable::getEnvironmentId, envId)
                .orderByAsc(EnvironmentVariable::getSortNo);

        List<EnvironmentVariable> variables = environmentVariableMapper.selectList(wrapper);
        Map<String, String> map = new LinkedHashMap<>();
        for (EnvironmentVariable v : variables) {
            map.put(v.getVarKey(), v.getVarValue());
        }
        return map;
    }

    // ───────────────────── 私有方法 ─────────────────────

    private Environment findById(Long envId) {
        Environment env = environmentMapper.selectById(envId);
        if (env == null) {
            throw new BusinessException(ErrorCode.ENV_NOT_FOUND, "环境不存在：" + envId);
        }
        return env;
    }

    /**
     * 全量替换变量：先删除所有旧变量，再批量插入新变量
     */
    private void replaceVariables(Long envId, List<EnvironmentVariableDTO> dtos) {
        // 删除所有旧变量
        LambdaQueryWrapper<EnvironmentVariable> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(EnvironmentVariable::getEnvironmentId, envId);
        environmentVariableMapper.delete(deleteWrapper);

        // 批量插入新变量
        int sortNo = 0;
        for (EnvironmentVariableDTO dto : dtos) {
            EnvironmentVariable variable = new EnvironmentVariable();
            variable.setEnvironmentId(envId);
            variable.setVarKey(dto.getVarKey());
            variable.setVarValue(dto.getVarValue());
            variable.setDescription(dto.getDescription());
            variable.setSortNo(sortNo++);
            environmentVariableMapper.insert(variable);
        }
    }

    /**
     * 查询环境的变量列表
     */
    private List<EnvironmentVariableDTO> listVariables(Long envId) {
        LambdaQueryWrapper<EnvironmentVariable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnvironmentVariable::getEnvironmentId, envId)
                .orderByAsc(EnvironmentVariable::getSortNo);

        List<EnvironmentVariable> variables = environmentVariableMapper.selectList(wrapper);
        List<EnvironmentVariableDTO> result = new ArrayList<>();
        for (EnvironmentVariable v : variables) {
            EnvironmentVariableDTO dto = new EnvironmentVariableDTO();
            dto.setVarKey(v.getVarKey());
            dto.setVarValue(v.getVarValue());
            dto.setDescription(v.getDescription());
            result.add(dto);
        }
        return result;
    }

    /**
     * 转换为基本响应（不含变量）
     */
    private EnvironmentResponse toBasicResponse(Environment env) {
        EnvironmentResponse response = new EnvironmentResponse();
        response.setId(env.getId());
        response.setProjectId(env.getProjectId());
        response.setName(env.getName());
        response.setDescription(env.getDescription());
        response.setIsCurrent(env.getIsCurrent());
        response.setCreatedAt(env.getCreatedAt());
        response.setUpdatedAt(env.getUpdatedAt());
        return response;
    }
}
