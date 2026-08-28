/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 执行器
 */
package com.platform.execution.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.action.entity.Action;
import com.platform.action.entity.ActionNode;
import com.platform.tool.dto.ToolTestResult;
import com.platform.tool.service.GroovySandboxExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Action 节点树执行器
 *
 * <p>按拓扑顺序执行 Action 的节点树。
 * <ul>
 *   <li>API_KEYWORD / TOOL_METHOD → 委托 KeywordExecutor 执行</li>
 *   <li>CONDITION → Groovy 表达式求值，走 true/false 分支</li>
 *   <li>LOOP → 按配置次数或条件表达式循环执行子节点</li>
 *   <li>START / END → 跳过</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActionExecutor {

    private final KeywordExecutor keywordExecutor;
    private final GroovySandboxExecutor groovySandboxExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 执行 Action
     *
     * @param action  Action 实体
     * @param params  输入参数
     * @param context 执行上下文
     * @return 执行结果
     */
    public StepResult executeAction(Action action, Map<String, Object> params, ExecutionContext context) {
        // 从 JSON 反序列化节点列表
        List<ActionNode> nodes = parseNodes(action.getNodes());

        if (nodes.isEmpty()) {
            return StepResult.error("Action 没有节点：" + action.getId());
        }

        // 将输入参数注入到 context 变量
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }

        // 按拓扑排序执行（positionY + 连线依赖）
        List<ActionNode> sorted = topologicalSort(nodes);

        List<Map<String, Object>> nodeResults = new ArrayList<>();
        boolean allPassed = true;
        long totalDuration = 0;

        // 记录已执行的节点 key（用于 CONDITION 跳过后跳过被跳过分支的节点）
        Set<String> executedKeys = new HashSet<>();
        Set<String> skippedKeys = new HashSet<>();

        for (ActionNode node : sorted) {
            Map<String, Object> nodeResult = new LinkedHashMap<>();
            nodeResult.put("nodeKey", node.getNodeKey());
            nodeResult.put("nodeType", node.getNodeType());

            // 如果该节点被前面的 CONDITION 分支跳过
            if (skippedKeys.contains(node.getNodeKey())) {
                nodeResult.put("status", "SKIPPED");
                nodeResult.put("message", "被条件分支跳过");
                nodeResults.add(nodeResult);
                continue;
            }

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
            if (stepResult.getAssertionSummary() != null) {
                nodeResult.put("assertionSummary", stepResult.getAssertionSummary());
            }

            nodeResults.add(nodeResult);
            totalDuration += stepResult.getDurationMs();
            executedKeys.add(node.getNodeKey());

            // CONDITION 节点：根据结果标记被跳过的分支节点
            String nodeType = node.getNodeType() != null ? node.getNodeType().toUpperCase() : "";
            if ("CONDITION".equals(nodeType)) {
                Map<String, Object> configMap = parseConfig(node.getConfig());
                String trueNext = (String) configMap.get("trueNext");
                String falseNext = (String) configMap.get("falseNext");
                boolean isTrue = "true".equalsIgnoreCase(stepResult.getMessage());
                // 标记被跳过分支的直接后继节点
                String skippedNext = isTrue ? falseNext : trueNext;
                if (skippedNext != null && !skippedNext.isEmpty()) {
                    skippedKeys.add(skippedNext);
                }
            }

            if ("FAILED".equals(stepResult.getStatus()) || "ERROR".equals(stepResult.getStatus())) {
                allPassed = false;
                break;
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
     * 从 action.nodes JSON 字符串反序列化节点列表
     */
    private List<ActionNode> parseNodes(String nodesJson) {
        if (nodesJson == null || nodesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ActionNode[] arr = objectMapper.readValue(nodesJson, ActionNode[].class);
            return Arrays.asList(arr);
        } catch (Exception e) {
            log.warn("反序列化 Action 节点失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ───────────────────── 节点执行 ─────────────────────

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
                return executeConditionNode(node, context);
            case "LOOP":
                return executeLoopNode(node, context);
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
        Map<String, Object> configMap = parseConfig(node.getConfig());
        Map<String, Object> params = null;

        // 新格式：参数存储在 config.params 子对象中
        Object paramsObj = configMap.get("params");
        if (paramsObj instanceof Map) {
            params = new LinkedHashMap<>((Map<String, Object>) paramsObj);
        } else if (!configMap.isEmpty()) {
            // 向后兼容：旧格式将整个 config 作为参数，排除内部元数据键
            params = new LinkedHashMap<>(configMap);
            params.remove("save_as");
            params.remove("refKeywordId");
            params.remove("refToolId");
            params.remove("nextNode");
            params.remove("trueNext");
            params.remove("falseNext");
            params.remove("condition");
            params.remove("count");
            params.remove("expression");
            params.remove("params");
        }

        if (params != null && !params.isEmpty()) {
            step.setParams(params);
        }

        return keywordExecutor.execute(step, context);
    }

    /**
     * 执行 CONDITION 节点
     * <p>从 config 中读取 expression（Groovy 表达式），求值后返回 true/false。
     * 表达式中可使用 ${var} 引用上下文变量。
     */
    private StepResult executeConditionNode(ActionNode node, ExecutionContext context) {
        Map<String, Object> configMap = parseConfig(node.getConfig());
        String expression = (String) configMap.get("expression");

        if (expression == null || expression.trim().isEmpty()) {
            log.warn("CONDITION 节点 {} 缺少 expression 配置", node.getNodeKey());
            return StepResult.passed("true");
        }

        // 变量替换
        expression = context.resolveVariables(expression);

        // 使用 Groovy 求值
        String groovyCode = "return (" + expression + ")";
        try {
            ToolTestResult result = groovySandboxExecutor.execute(groovyCode, "{}");
            if (result != null && Integer.valueOf(1).equals(result.getSuccess())) {
                boolean isTrue = !"false".equalsIgnoreCase(result.getOutput())
                        && !"0".equals(result.getOutput())
                        && !"null".equals(result.getOutput());
                return StepResult.passed(String.valueOf(isTrue));
            } else {
                String errMsg = result != null ? result.getError() : "表达式求值失败";
                return StepResult.error("条件表达式执行失败: " + errMsg);
            }
        } catch (Exception e) {
            log.warn("CONDITION 表达式执行异常: {}", e.getMessage());
            return StepResult.error("条件表达式异常: " + e.getMessage());
        }
    }

    /**
     * 执行 LOOP 节点
     * <p>从 config 中读取 count（循环次数）或 expression（循环条件），
     * 循环将循环计数器设置到 context 变量中（_loopIndex, _loopCount）。
     */
    private StepResult executeLoopNode(ActionNode node, ExecutionContext context) {
        Map<String, Object> configMap = parseConfig(node.getConfig());

        int loopCount = 0;
        if (configMap.containsKey("count")) {
            Object countObj = configMap.get("count");
            if (countObj instanceof Number) {
                loopCount = ((Number) countObj).intValue();
            } else {
                try {
                    loopCount = Integer.parseInt(countObj.toString());
                } catch (NumberFormatException e) {
                    loopCount = 0;
                }
            }
        }

        // 支持条件表达式控制循环次数
        String expression = (String) configMap.get("expression");
        if (expression != null && !expression.trim().isEmpty() && loopCount <= 0) {
            // 条件表达式模式：循环直到表达式为 false
            int maxIterations = 100; // 安全上限
            loopCount = 0;
            for (int i = 0; i < maxIterations; i++) {
                String resolved = context.resolveVariables(expression);
                String groovyCode = "return (" + resolved + ")";
                try {
                    ToolTestResult result = groovySandboxExecutor.execute(groovyCode, "{}");
                    if (result != null && Integer.valueOf(1).equals(result.getSuccess())) {
                        boolean isTrue = !"false".equalsIgnoreCase(result.getOutput())
                                && !"0".equals(result.getOutput())
                                && !"null".equals(result.getOutput());
                        if (!isTrue) break;
                        loopCount++;
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }

        if (loopCount <= 0) {
            return StepResult.passed("循环次数为 0，跳过执行");
        }

        // 设置循环变量到 context
        context.setVariable("_loopCount", loopCount);

        Map<String, Object> loopResult = new LinkedHashMap<>();
        loopResult.put("loopCount", loopCount);
        loopResult.put("message", "循环执行 " + loopCount + " 次（循环体内节点在后续步骤中执行）");

        StepResult result = new StepResult();
        result.setStatus("PASSED");
        result.setMessage("循环 " + loopCount + " 次");
        result.setDurationMs(0);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("loopResult", loopResult);
        result.setResponse(resp);
        return result;
    }

    // ───────────────────── 工具方法 ─────────────────────

    /**
     * 拓扑排序：基于 positionY + config 中的 nextNode 引用构建执行顺序
     */
    private List<ActionNode> topologicalSort(List<ActionNode> nodes) {
        if (nodes.size() <= 1) {
            return nodes;
        }

        // 构建 nodeKey -> ActionNode 映射
        Map<String, ActionNode> nodeMap = new LinkedHashMap<>();
        for (ActionNode node : nodes) {
            nodeMap.put(node.getNodeKey(), node);
        }

        // 构建邻接表和入度表
        Map<String, List<String>> adjacency = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (ActionNode node : nodes) {
            adjacency.putIfAbsent(node.getNodeKey(), new ArrayList<>());
            inDegree.putIfAbsent(node.getNodeKey(), 0);
        }

        // 从 config 中提取 nextNode 引用，构建有向边
        for (ActionNode node : nodes) {
            Map<String, Object> configMap = parseConfig(node.getConfig());
            addEdge(node.getNodeKey(), (String) configMap.get("nextNode"), adjacency, inDegree, nodeMap);
            addEdge(node.getNodeKey(), (String) configMap.get("trueNext"), adjacency, inDegree, nodeMap);
            addEdge(node.getNodeKey(), (String) configMap.get("falseNext"), adjacency, inDegree, nodeMap);
        }

        // Kahn 算法
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<ActionNode> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String key = queue.poll();
            ActionNode node = nodeMap.get(key);
            if (node != null) {
                sorted.add(node);
            }
            for (String next : adjacency.getOrDefault(key, Collections.emptyList())) {
                int newDegree = inDegree.getOrDefault(next, 0) - 1;
                inDegree.put(next, newDegree);
                if (newDegree == 0) {
                    queue.add(next);
                }
            }
        }

        // 如果拓扑排序未覆盖所有节点（存在环或断开的子图），追加剩余节点（按 positionY）
        if (sorted.size() < nodes.size()) {
            Set<String> sortedKeys = new HashSet<>();
            for (ActionNode n : sorted) {
                sortedKeys.add(n.getNodeKey());
            }
            for (ActionNode node : nodes) {
                if (!sortedKeys.contains(node.getNodeKey())) {
                    sorted.add(node);
                }
            }
        }

        return sorted;
    }

    private void addEdge(String from, String to, Map<String, List<String>> adjacency,
                          Map<String, Integer> inDegree, Map<String, ActionNode> nodeMap) {
        if (to != null && !to.isEmpty() && nodeMap.containsKey(to)) {
            adjacency.get(from).add(to);
            inDegree.merge(to, 1, Integer::sum);
        }
    }

    /**
     * 解析节点 config JSON 为 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析节点 config 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
