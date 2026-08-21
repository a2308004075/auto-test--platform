package com.platform.environment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.environment.dto.*;
import com.platform.environment.entity.Environment;
import com.platform.environment.mapper.EnvironmentMapper;
import com.platform.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

/**
 * 环境配置管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentService {

    private final EnvironmentMapper environmentMapper;
    private final ProjectService projectService;

    /**
     * 查询项目下的环境列表
     */
    public List<EnvironmentResponse> listByProject(Long projectId) {
        LambdaQueryWrapper<Environment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Environment::getProjectId, projectId);
        wrapper.orderByDesc(Environment::getIsCurrent, Environment::getCreatedAt);

        List<Environment> list = environmentMapper.selectList(wrapper);
        List<EnvironmentResponse> result = new ArrayList<>();
        for (Environment env : list) {
            result.add(toResponse(env));
        }
        return result;
    }

    /**
     * 创建环境
     */
    public EnvironmentResponse create(EnvironmentCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        Environment env = new Environment();
        env.setProjectId(request.getProjectId());
        env.setName(request.getName());
        env.setHost(request.getHost());
        env.setPort(request.getPort());
        env.setDatabaseName(request.getDatabaseName());
        env.setUsername(request.getUsername());
        env.setPassword(request.getPassword());
        env.setConfigJson(request.getConfigJson());
        env.setIsCurrent(0);

        environmentMapper.insert(env);
        return toResponse(env);
    }

    /**
     * 更新环境
     */
    public EnvironmentResponse update(Long envId, EnvironmentUpdateRequest request) {
        Environment env = findById(envId);

        if (StringUtils.hasText(request.getName())) {
            env.setName(request.getName());
        }
        if (request.getHost() != null) {
            env.setHost(request.getHost());
        }
        if (request.getPort() != null) {
            env.setPort(request.getPort());
        }
        if (request.getDatabaseName() != null) {
            env.setDatabaseName(request.getDatabaseName());
        }
        if (request.getUsername() != null) {
            env.setUsername(request.getUsername());
        }
        if (request.getPassword() != null) {
            env.setPassword(request.getPassword());
        }
        if (request.getConfigJson() != null) {
            env.setConfigJson(request.getConfigJson());
        }

        environmentMapper.updateById(env);
        return toResponse(env);
    }

    /**
     * 删除环境
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

        return toResponse(env);
    }

    /**
     * 测试环境连接
     */
    public TestResult testConnection(Long envId) {
        Environment env = findById(envId);
        return doTestConnection(env);
    }

    /**
     * 测试连接（使用请求参数）
     */
    public TestResult testConnectionByParams(EnvironmentCreateRequest request) {
        Environment temp = new Environment();
        BeanUtils.copyProperties(request, temp);
        return doTestConnection(temp);
    }

    /**
     * 获取环境详情
     */
    public EnvironmentResponse getDetail(Long envId) {
        return toResponse(findById(envId));
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
        return toResponse(env);
    }

    // ───────────────────── 私有方法 ─────────────────────

    private Environment findById(Long envId) {
        Environment env = environmentMapper.selectById(envId);
        if (env == null) {
            throw new BusinessException(ErrorCode.ENV_NOT_FOUND, "环境不存在：" + envId);
        }
        return env;
    }

    private TestResult doTestConnection(Environment env) {
        if (!StringUtils.hasText(env.getHost())) {
            return TestResult.fail("主机地址不能为空");
        }

        String url = buildJdbcUrl(env);
        long start = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(url, env.getUsername(), env.getPassword())) {
            long elapsed = System.currentTimeMillis() - start;
            return TestResult.ok(elapsed);
        } catch (Exception e) {
            log.warn("环境连接测试失败 [{}]: {}", env.getName(), e.getMessage());
            return TestResult.fail("连接失败：" + e.getMessage());
        }
    }

    private String buildJdbcUrl(Environment env) {
        StringBuilder sb = new StringBuilder("jdbc:mysql://");
        sb.append(env.getHost());
        if (env.getPort() != null) {
            sb.append(":").append(env.getPort());
        }
        if (StringUtils.hasText(env.getDatabaseName())) {
            sb.append("/").append(env.getDatabaseName());
        }
        sb.append("?connectTimeout=5000&socketTimeout=5000&useSSL=false&characterEncoding=UTF-8");
        return sb.toString();
    }

    private EnvironmentResponse toResponse(Environment env) {
        EnvironmentResponse response = new EnvironmentResponse();
        BeanUtils.copyProperties(env, response);
        response.setPassword(null); // 不返回密码
        return response;
    }
}
