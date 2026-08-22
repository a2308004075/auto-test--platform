/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 执行器
 */
package com.platform.execution.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.action.entity.Action;
import com.platform.action.entity.ActionNode;
import com.platform.action.mapper.ActionNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Action 节点树执行器
 *
 * <p>按拓扑顺序执行 Action 的节点树。
 * 对于 API_KEYWORD / TOOL_METHOD 类型节点，委托 KeywordExecutor 执行。
 * CONDITION / LOOP 类型节点暂作简化处理。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActionExecutor {

    private final ActionNodeMapper actionNodeMapper;
    private final KeywordExecutor keywordExecutor;

    /**
     * 执行 Action
     *
     * @param action  Action 实体
     * @param params  输入参数
     * @param context 执行上下文
     * @return 执行结果
     */
    public StepResult executeAction(Action action, Map<String, Object> params, ExecutionContext context) {
        // 查询节点列表（按 positionY 排序）
        LambdaQueryWrapper<ActionNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActionNode::getActionId, action.getId())
                .orderByAsc(ActionNode::getPositionY);
        List<ActionNode> nodes = actionNodeMapper.selectList(wrapper);

        if (nodes.isEmpty()) {
            return StepResult.error("Action 没有节点：" + action.getId());
        }

        // 将输入参数注入到 context 变量
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }

        List<Map<String, Object>> nodeResults = new ArrayList<>();
        boolean allPassed = true;
        long totalDuration = 0;

        for (ActionNode node : nodes) {
            Map<String, Object> nodeResult = new LinkedHashMap<>();
            nodeResult.put("nodeKey", node.getNodeKey());
            nodeResult.put("nodeType", node.getNodeType());

            StepResult stepResult = executeNode(node, context);
            nodeResult.put("status", stepResult.getStatus());
            nodeResult.put("message", stepResult.getMessage());
            nodeResult.put("durationMs", stepResult.getDurationMs());

            if (stepResult.getRequest() != null) {
                nodeResult.put("request", stepResult.getRequest());
            }
            if (stepResult.getResponse() != null) {
                nodeResult.put("response", stepResult.getResponse());
            }

            nodeResults.add(nodeResult);
            totalDuration += stepResult.getDurationMs();

            if ("FAILED".equals(stepResult.getStatus()) || "ERROR".equals(stepResult.getStatus())) {
                allPassed = false;
                break; // 遇到失败则停止后续节点执行
            }
        }

        StepResult result = new StepResult();
        result.setStatus(allPassed ? "PASSED" : "FAILED");
        result.setMessage(allPassed ? "Action 执行成功" : "Action 执行失败（节点异常）");
        result.setDurationMs(totalDuration);
        Map<String, Object> respDetail = new LinkedHashMap<>();
        respDetail.put("nodeResults", nodeResults);
        result.setResponse(respDetail);
        return result;
    }

    /**
     * 执行单个 Action 节点
     */
    private StepResult executeNode(ActionNode node, ExecutionContext context) {
        String nodeType = node.getNodeType() != null ? node.getNodeType().toUpperCase() : "";

        switch (nodeType) {
            case "API_KEYWORD":
            case "TOOL_METHOD":
                return executeKeywordNode(node, context);
            case "START":
            case "END":
                return StepResult.passed("节点跳过：" + nodeType);
            case "CONDITION":
            case "LOOP":
                log.warn("节点类型 {} 暂未实现，跳过执行", nodeType);
                return StepResult.passed("节点类型 " + nodeType + " 暂未实现，已跳过");
            default:
                return StepResult.passed("未知节点类型，已跳过：" + nodeType);
        }
    }

    /**
     * 执行关键字类型节点（API_KEYWORD / TOOL_METHOD）
     */
    private StepResult executeKeywordNode(ActionNode node, ExecutionContext context) {
        // 优先使用 refKeywordId
        Long keywordId = node.getRefKeywordId();
        if (keywordId == null) {
            // 对于 TOOL_METHOD 类型，使用 refToolId（需要通过 ToolMethod.keyword 查找对应 Keyword）
            if (node.getRefToolId() != null) {
                keywordId = node.getRefToolId();
            }
        }

        if (keywordId == null) {
            return StepResult.error("节点未引用关键字：" + node.getNodeKey());
        }

        // 构建步骤节点
        StepNode step = new StepNode();
        step.setKeywordId(keywordId);
        step.setName(node.getNodeKey());

        // 解析节点 config 中的参数
        if (node.getConfig() != null && !node.getConfig().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(node.getConfig(), Map.class);
                step.setParams(config);
            } catch (Exception e) {
                log.warn("解析节点 config 失败: {}", e.getMessage());
            }
        }

        return keywordExecutor.execute(step, context);
    }
}
