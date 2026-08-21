package com.platform.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.common.response.PageResponse;
import com.platform.project.service.ProjectService;
import com.platform.tool.dto.*;
import com.platform.tool.entity.ToolMethod;
import com.platform.tool.mapper.ToolMethodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具方法管理服务
 */
@Service
@RequiredArgsConstructor
public class ToolMethodService {

    private final ToolMethodMapper toolMethodMapper;
    private final GroovySandboxExecutor groovySandboxExecutor;
    private final ProjectService projectService;

    /**
     * 分页查询工具方法
     */
    public PageResponse<ToolMethodResponse> list(Long projectId, String category,
                                                   String keyword, int page, int pageSize) {
        LambdaQueryWrapper<ToolMethod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToolMethod::getProjectId, projectId);
        if (StringUtils.hasText(category)) {
            wrapper.eq(ToolMethod::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ToolMethod::getName, keyword)
                    .or().like(ToolMethod::getDescription, keyword));
        }
        wrapper.orderByDesc(ToolMethod::getCreatedAt);

        Page<ToolMethod> pageParam = new Page<>(page, pageSize);
        Page<ToolMethod> result = toolMethodMapper.selectPage(pageParam, wrapper);

        List<ToolMethodResponse> records = new ArrayList<>();
        for (ToolMethod tm : result.getRecords()) {
            records.add(toResponse(tm));
        }
        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    /**
     * 创建工具方法
     */
    public ToolMethodResponse create(ToolMethodCreateRequest request) {
        projectService.findActiveById(request.getProjectId());

        // 名称唯一性检查
        LambdaQueryWrapper<ToolMethod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToolMethod::getProjectId, request.getProjectId())
                .eq(ToolMethod::getName, request.getName());
        if (toolMethodMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.TOOL_NAME_DUPLICATE,
                    "工具方法名称已存在：" + request.getName());
        }

        ToolMethod tm = new ToolMethod();
        tm.setProjectId(request.getProjectId());
        tm.setName(request.getName());
        tm.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory() : "CUSTOM");
        tm.setDescription(request.getDescription());
        tm.setParamDefinitions(request.getParamDefinitions());
        tm.setReturnType(request.getReturnType());
        tm.setCode(request.getCode());
        tm.setIsActive(1);
        toolMethodMapper.insert(tm);

        return toResponse(tm);
    }

    /**
     * 更新工具方法
     */
    public ToolMethodResponse update(Long toolId, ToolMethodUpdateRequest request) {
        ToolMethod tm = findById(toolId);

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(tm.getName())) {
            LambdaQueryWrapper<ToolMethod> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ToolMethod::getProjectId, tm.getProjectId())
                    .eq(ToolMethod::getName, request.getName())
                    .ne(ToolMethod::getId, toolId);
            if (toolMethodMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.TOOL_NAME_DUPLICATE,
                        "工具方法名称已存在：" + request.getName());
            }
            tm.setName(request.getName());
        }
        if (request.getDescription() != null) {
            tm.setDescription(request.getDescription());
        }
        if (request.getParamDefinitions() != null) {
            tm.setParamDefinitions(request.getParamDefinitions());
        }
        if (request.getReturnType() != null) {
            tm.setReturnType(request.getReturnType());
        }
        if (request.getCode() != null) {
            tm.setCode(request.getCode());
        }

        toolMethodMapper.updateById(tm);
        return toResponse(tm);
    }

    /**
     * 获取工具方法详情
     */
    public ToolMethodResponse getById(Long toolId) {
        return toResponse(findById(toolId));
    }

    /**
     * 删除工具方法（软删除）
     */
    public void delete(Long toolId) {
        findById(toolId);
        toolMethodMapper.deleteById(toolId);
    }

    /**
     * 在线测试工具方法
     */
    public ToolTestResult testTool(Long toolId, ToolTestRequest request) {
        ToolMethod tm = findById(toolId);

        ToolTestResult result = groovySandboxExecutor.execute(tm.getCode(), request.getTestInput());

        // 保存测试输入和结果
        tm.setTestInput(request.getTestInput());
        tm.setTestResult(Integer.valueOf(1).equals(result.getSuccess()) ? result.getOutput() : result.getError());
        toolMethodMapper.updateById(tm);

        return result;
    }

    // ───────────────────── 私有方法 ─────────────────────

    private ToolMethod findById(Long toolId) {
        ToolMethod tm = toolMethodMapper.selectById(toolId);
        if (tm == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具方法不存在：" + toolId);
        }
        return tm;
    }

    private ToolMethodResponse toResponse(ToolMethod tm) {
        ToolMethodResponse resp = new ToolMethodResponse();
        BeanUtils.copyProperties(tm, resp);
        return resp;
    }
}
