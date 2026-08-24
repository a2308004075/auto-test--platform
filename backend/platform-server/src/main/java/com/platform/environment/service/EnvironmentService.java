/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 环境管理服务
 */
package com.platform.environment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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
    private final ProjectVariableService projectVariableService;

    // ponytail: 每次调用创建新 engine 保证线程安全，Nashorn 创建开销可接受
    private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();

    private static final List<String> JS_BLACKLIST = Arrays.asList(
            "Runtime", "ProcessBuilder", "System.exit",
            "java.io", "java.net", "java.lang.reflect",
            "Packages", "Java.type", "importPackage", "importClass",
            "load(", "loadWithNewGlobal"
    );

    /**
     * 查询项目下的环境列表（基本信息，不含变量详情）
     */
    public List<EnvironmentResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<Environment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Environment::getProjectId, projectId);
        wrapper.orderByDesc(Environment::getCreatedAt);

        List<Environment> list = environmentMapper.selectList(wrapper);
        List<EnvironmentResponse> result = new ArrayList<>();
        for (Environment env : list) {
            result.add(toBasicResponse(env));
        }
        return result;
    }

    /**
     * 创建环境（自动预置 host、authorization 两个固定变量，用户可通过编辑页添加自定义变量）
     */
    @Transactional(rollbackFor = Exception.class)
    public EnvironmentResponse create(EnvironmentCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        Environment env = new Environment();
        env.setProjectId(request.getProjectId());
        env.setName(request.getName());
        env.setDescription(request.getDescription());

        environmentMapper.insert(env);

        // 预置固定变量 host、authorization
        insertPresetVariable(env.getId(), "host", "", "text", 0);
        insertPresetVariable(env.getId(), "authorization", "", "text", 1);

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
     * 获取环境详情（含变量列表）
     */
    public EnvironmentResponse getDetail(Long envId) {
        Environment env = findById(envId);
        EnvironmentResponse response = toBasicResponse(env);
        response.setVariables(listVariables(envId));
        return response;
    }

    /**
     * 获取环境变量 Map（供执行引擎和调试服务使用）
     *
     * @param envId 环境 ID
     * @return 变量名 → 变量值 的 Map
     */
    public Map<String, String> getVariablesAsMap(Long envId) {
        Environment env = findById(envId);
        Map<String, String> map = new LinkedHashMap<>();

        // 先加载项目全局变量（低优先级）
        try {
            map.putAll(projectVariableService.getVariablesAsMap(env.getProjectId()));
        } catch (Exception e) {
            log.warn("加载项目全局变量失败: {}", e.getMessage());
        }

        // 再叠加环境变量（高优先级，同名时覆盖全局变量）
        LambdaQueryWrapper<EnvironmentVariable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnvironmentVariable::getEnvironmentId, envId)
                .orderByAsc(EnvironmentVariable::getSortNo);

        List<EnvironmentVariable> variables = environmentVariableMapper.selectList(wrapper);
        for (EnvironmentVariable v : variables) {
            if ("script".equals(v.getDataType())) {
                map.put(v.getVarKey(), evaluateScript(v.getVarKey(), v.getVarValue()));
            } else {
                map.put(v.getVarKey(), v.getVarValue());
            }
        }
        return map;
    }

    /**
     * 执行脚本类型变量，返回脚本输出结果
     */
    private String evaluateScript(String varKey, String code) {
        if (code == null || code.trim().isEmpty()) {
            return "";
        }
        // 安全检查
        for (String blocked : JS_BLACKLIST) {
            if (code.contains(blocked)) {
                log.warn("环境变量 [{}] 脚本安全检查失败：包含禁止操作 {}", varKey, blocked);
                return "";
            }
        }
        try {
            ScriptEngine engine = ENGINE_MANAGER.getEngineByName("javascript");
            Object result = engine.eval(code);
            return result != null ? result.toString() : "";
        } catch (ScriptException e) {
            log.warn("环境变量 [{}] 脚本执行异常: {}", varKey, e.getMessage());
            return "";
        }
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

        // 从请求中提取 host、authorization 的值（固定变量，键名锁定）
        String hostValue = "";
        String authorizationValue = "";
        List<EnvironmentVariableDTO> customVars = new ArrayList<>();
        for (EnvironmentVariableDTO dto : dtos) {
            String key = dto.getVarKey();
            if ("host".equals(key)) {
                hostValue = dto.getVarValue() != null ? dto.getVarValue() : "";
            } else if ("authorization".equals(key)) {
                authorizationValue = dto.getVarValue() != null ? dto.getVarValue() : "";
            } else {
                customVars.add(dto);
            }
        }

        // 先插入固定变量 host、authorization
        insertPresetVariable(envId, "host", hostValue, "text", 0);
        insertPresetVariable(envId, "authorization", authorizationValue, "text", 1);

        // 再插入自定义变量
        int sortNo = 2;
        for (EnvironmentVariableDTO dto : customVars) {
            EnvironmentVariable variable = new EnvironmentVariable();
            variable.setEnvironmentId(envId);
            variable.setVarKey(dto.getVarKey());
            variable.setVarValue(dto.getVarValue());
            variable.setDataType(dto.getDataType() != null ? dto.getDataType() : "text");
            variable.setDescription(dto.getDescription());
            variable.setSortNo(sortNo++);
            environmentVariableMapper.insert(variable);
        }
    }

    /**
     * 插入预置固定变量
     */
    private void insertPresetVariable(Long envId, String key, String value, String dataType, int sortNo) {
        EnvironmentVariable variable = new EnvironmentVariable();
        variable.setEnvironmentId(envId);
        variable.setVarKey(key);
        variable.setVarValue(value);
        variable.setDataType(dataType);
        variable.setSortNo(sortNo);
        environmentVariableMapper.insert(variable);
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
            log.info("[DEBUG] envVar id={}, key={}, value={}", v.getId(), v.getVarKey(), v.getVarValue());
            EnvironmentVariableDTO dto = new EnvironmentVariableDTO();
            dto.setVarKey(v.getVarKey());
            dto.setVarValue(v.getVarValue());
            dto.setDataType(v.getDataType());
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
        response.setCreatedAt(env.getCreatedAt());
        response.setUpdatedAt(env.getUpdatedAt());
        return response;
    }
}
