/**
 * @author HXN
 * @date 2026-08-24
 * @description 项目全局变量管理服务
 */
package com.platform.environment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.environment.dto.ProjectVariableDTO;
import com.platform.environment.entity.ProjectVariable;
import com.platform.environment.mapper.ProjectVariableMapper;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;

/**
 * 项目全局变量管理服务
 * 全局变量不绑定环境，整个项目任何地方可引用
 * 环境变量优先级高于全局变量（同名时环境变量覆盖全局）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectVariableService {

    private final ProjectVariableMapper projectVariableMapper;
    private final ProjectService projectService;

    // ponytail: 脚本安全检查逻辑与 EnvironmentService 重复，若 JS_BLACKLIST 变更需同步更新
    private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();

    private static final List<String> JS_BLACKLIST = Arrays.asList(
            "Runtime", "ProcessBuilder", "System.exit",
            "java.io", "java.net", "java.lang.reflect",
            "Packages", "Java.type", "importPackage", "importClass",
            "load(", "loadWithNewGlobal"
    );

    /**
     * 查询项目下的全局变量列表
     */
    public List<ProjectVariableDTO> list(Long projectId) {
        LambdaQueryWrapper<ProjectVariable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectVariable::getProjectId, projectId)
                .orderByAsc(ProjectVariable::getSortNo);

        List<ProjectVariable> variables = projectVariableMapper.selectList(wrapper);
        List<ProjectVariableDTO> result = new ArrayList<>();
        for (ProjectVariable v : variables) {
            ProjectVariableDTO dto = new ProjectVariableDTO();
            dto.setVarKey(v.getVarKey());
            dto.setVarValue(v.getVarValue());
            dto.setDataType(v.getDataType());
            dto.setDescription(v.getDescription());
            result.add(dto);
        }
        return result;
    }

    /**
     * 全量替换项目全局变量（先删后插）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long projectId, List<ProjectVariableDTO> dtos) {
        projectService.findActiveById(projectId);

        // 删除所有旧变量
        LambdaQueryWrapper<ProjectVariable> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ProjectVariable::getProjectId, projectId);
        projectVariableMapper.delete(deleteWrapper);

        // 批量插入新变量
        int sortNo = 0;
        for (ProjectVariableDTO dto : dtos) {
            ProjectVariable variable = new ProjectVariable();
            variable.setProjectId(projectId);
            variable.setVarKey(dto.getVarKey());
            variable.setVarValue(dto.getVarValue());
            variable.setDataType(dto.getDataType() != null ? dto.getDataType() : "text");
            variable.setDescription(dto.getDescription());
            variable.setSortNo(sortNo++);
            projectVariableMapper.insert(variable);
        }
    }

    /**
     * 获取项目全局变量 Map（供 EnvironmentService 合并使用）
     *
     * @param projectId 项目 ID
     * @return 变量名 → 变量值 的 Map
     */
    public Map<String, String> getVariablesAsMap(Long projectId) {
        LambdaQueryWrapper<ProjectVariable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectVariable::getProjectId, projectId)
                .orderByAsc(ProjectVariable::getSortNo);

        List<ProjectVariable> variables = projectVariableMapper.selectList(wrapper);
        Map<String, String> map = new LinkedHashMap<>();
        for (ProjectVariable v : variables) {
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
        for (String blocked : JS_BLACKLIST) {
            if (code.contains(blocked)) {
                log.warn("项目变量 [{}] 脚本安全检查失败：包含禁止操作 {}", varKey, blocked);
                return "";
            }
        }
        try {
            ScriptEngine engine = ENGINE_MANAGER.getEngineByName("javascript");
            Object result = engine.eval(code);
            return result != null ? result.toString() : "";
        } catch (ScriptException e) {
            log.warn("项目变量 [{}] 脚本执行异常: {}", varKey, e.getMessage());
            return "";
        }
    }
}
