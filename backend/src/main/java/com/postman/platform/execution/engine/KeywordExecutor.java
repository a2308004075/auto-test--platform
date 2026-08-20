package com.postman.platform.execution.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postman.platform.action.entity.Action;
import com.postman.platform.action.mapper.ActionMapper;
import com.postman.platform.apidoc.entity.Api;
import com.postman.platform.apidoc.mapper.ApiMapper;
import com.postman.platform.common.util.SpringContextHolder;
import com.postman.platform.keyword.entity.ApiKeyword;
import com.postman.platform.keyword.entity.Keyword;
import com.postman.platform.keyword.mapper.ApiKeywordMapper;
import com.postman.platform.keyword.mapper.KeywordMapper;
import com.postman.platform.tool.entity.ToolMethod;
import com.postman.platform.tool.mapper.ToolMethodMapper;
import com.postman.platform.tool.dto.ToolTestResult;
import com.postman.platform.tool.service.GroovySandboxExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 关键字执行器
 *
 * <p>统一入口，根据关键字类型分发执行：
 * <ul>
 *   <li>API → HttpClientEngine（查 ApiKeyword → Api → 发送 HTTP 请求）</li>
 *   <li>TOOL → GroovySandboxExecutor（查 ToolMethod → 执行 Groovy 代码）</li>
 *   <li>ACTION → ActionExecutor（查 Action → 执行节点树）</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeywordExecutor {

    private final KeywordMapper keywordMapper;
    private final ApiKeywordMapper apiKeywordMapper;
    private final ApiMapper apiMapper;
    private final ToolMethodMapper toolMethodMapper;
    private final ActionMapper actionMapper;
    private final HttpClientEngine httpClientEngine;
    private final AssertionEngine assertionEngine;
    private final GroovySandboxExecutor groovySandboxExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 执行单个关键字步骤
     *
     * @param step    步骤节点（含 keywordId + params + assertions）
     * @param context 执行上下文
     * @return 步骤执行结果
     */
    public StepResult execute(StepNode step, ExecutionContext context) {
        if (step.getKeywordId() == null) {
            return StepResult.error("步骤缺少 keywordId");
        }

        Keyword keyword = keywordMapper.selectById(step.getKeywordId());
        if (keyword == null) {
            return StepResult.error("关键字不存在：" + step.getKeywordId());
        }

        String type = keyword.getType() != null ? keyword.getType().toUpperCase() : "";
        switch (type) {
            case "API":
                return executeApiKeyword(step, keyword, context);
            case "TOOL":
                return executeToolKeyword(step, keyword, context);
            case "ACTION":
                return executeActionKeyword(step, keyword, context);
            case "TEST_CASE":
                return StepResult.error("TEST_CASE 类型关键字暂不支持嵌套执行");
            default:
                return StepResult.error("未知关键字类型：" + type);
        }
    }

    /**
     * 执行 API 类型关键字
     */
    @SuppressWarnings("unchecked")
    private StepResult executeApiKeyword(StepNode step, Keyword keyword, ExecutionContext context) {
        // 查 ApiKeyword 绑定记录
        ApiKeyword apiKeyword = null;
        List<ApiKeyword> bindings = apiKeywordMapper.selectList(
                new LambdaQueryWrapper<ApiKeyword>()
                        .eq(ApiKeyword::getKeywordId, keyword.getId()));
        if (!bindings.isEmpty()) {
            apiKeyword = bindings.get(0);
        }

        if (apiKeyword == null || apiKeyword.getApiId() == null) {
            return StepResult.error("API 关键字未绑定接口：" + keyword.getName());
        }

        // 查 Api 接口定义
        Api api = apiMapper.selectById(apiKeyword.getApiId());
        if (api == null) {
            return StepResult.error("接口定义不存在：" + apiKeyword.getApiId());
        }

        // 解析 headers（Api.headers 是 JSON 字符串）
        Map<String, String> headers = new LinkedHashMap<>();
        if (api.getHeaders() != null && !api.getHeaders().isEmpty()) {
            try {
                Map<String, Object> apiHeaders = objectMapper.readValue(api.getHeaders(),
                        new TypeReference<Map<String, Object>>() {});
                for (Map.Entry<String, Object> e : apiHeaders.entrySet()) {
                    headers.put(e.getKey(), e.getValue() != null ? e.getValue().toString() : "");
                }
            } catch (Exception e) {
                log.warn("解析接口 headers 失败: {}", e.getMessage());
            }
        }

        // 合并 step.params 中的额外 headers
        if (step.getParams() != null) {
            Object extraHeaders = step.getParams().get("headers");
            if (extraHeaders instanceof Map) {
                ((Map<String, Object>) extraHeaders).forEach((k, v) ->
                        headers.put(k, v != null ? v.toString() : ""));
            }
        }

        // 构建请求体
        String body = null;
        if (step.getParams() != null) {
            Object bodyParam = step.getParams().get("body");
            if (bodyParam != null) {
                try {
                    body = bodyParam instanceof String ? (String) bodyParam : objectMapper.writeValueAsString(bodyParam);
                } catch (Exception e) {
                    body = bodyParam.toString();
                }
            }
        }
        if (body == null && api.getRequestBody() != null && !api.getRequestBody().isEmpty()) {
            body = api.getRequestBody();
        }

        // 路径参数替换（step.params 中的路径变量）
        String path = api.getPath();
        if (step.getParams() != null) {
            for (Map.Entry<String, Object> entry : step.getParams().entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                if (path.contains(placeholder) && entry.getValue() != null) {
                    path = path.replace(placeholder, entry.getValue().toString());
                }
            }
        }

        // 发送 HTTP 请求
        StepResult result = httpClientEngine.execute(
                api.getHttpMethod(), path, headers, body, context);

        // 执行断言（步骤级断言 + 关键字级 responseAssertion）
        if (result.getResponse() != null) {
            List<AssertionItem> allAssertions = new ArrayList<>();
            if (step.getAssertions() != null) {
                allAssertions.addAll(step.getAssertions());
            }
            // 解析关键字级断言
            if (apiKeyword.getResponseAssertion() != null && !apiKeyword.getResponseAssertion().isEmpty()) {
                try {
                    List<AssertionItem> kwAssertions = objectMapper.readValue(
                            apiKeyword.getResponseAssertion(),
                            new TypeReference<List<AssertionItem>>() {});
                    allAssertions.addAll(kwAssertions);
                } catch (Exception e) {
                    log.warn("解析关键字级断言失败: {}", e.getMessage());
                }
            }

            if (!allAssertions.isEmpty()) {
                String assertionSummary = assertionEngine.evaluate(allAssertions, result.getResponse());
                result.setAssertionSummary(assertionSummary);
                // 如果断言失败，标记为 FAILED
                if (assertionSummary != null && assertionSummary.contains("失败")) {
                    result.setStatus("FAILED");
                    result.setMessage("断言失败: " + assertionSummary);
                }
            }
        }

        // 将响应中的输出变量设置到 context（简化：从响应体提取指定字段）
        if (step.getParams() != null && step.getParams().containsKey("output")) {
            Object outputConfig = step.getParams().get("output");
            if (outputConfig instanceof Map && result.getResponse() != null) {
                Map<String, Object> outputMap = (Map<String, Object>) outputConfig;
                Object respBody = result.getResponse().get("body");
                if (respBody != null) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(respBody.toString(), Map.class);
                        for (Map.Entry<String, Object> e : outputMap.entrySet()) {
                            String varName = e.getKey();
                            String jsonPath = e.getValue() != null ? e.getValue().toString() : varName;
                            Object val = extractByPath(bodyMap, jsonPath);
                            if (val != null) {
                                context.setVariable(varName, val);
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("提取输出变量失败: {}", ex.getMessage());
                    }
                }
            }
        }

        return result;
    }

    /**
     * 执行 TOOL 类型关键字
     */
    private StepResult executeToolKeyword(StepNode step, Keyword keyword, ExecutionContext context) {
        Long refId = keyword.getRefId();
        if (refId == null) {
            return StepResult.error("TOOL 关键字缺少 refId：" + keyword.getName());
        }

        ToolMethod tool = toolMethodMapper.selectById(refId);
        if (tool == null) {
            return StepResult.error("工具方法不存在：" + refId);
        }

        // 序列化参数为 JSON 输入
        String inputJson;
        try {
            inputJson = step.getParams() != null
                    ? objectMapper.writeValueAsString(step.getParams())
                    : "{}";
        } catch (Exception e) {
            inputJson = "{}";
        }

        // 执行 Groovy 代码
        long toolStart = System.currentTimeMillis();
        ToolTestResult toolResult = groovySandboxExecutor.execute(tool.getCode(), inputJson);
        long toolElapsed = System.currentTimeMillis() - toolStart;

        StepResult result = new StepResult();
        if (toolResult != null && Boolean.TRUE.equals(toolResult.getSuccess())) {
            result.setStatus("PASSED");
            result.setMessage("工具方法执行成功");
            Map<String, Object> respDetail = new LinkedHashMap<>();
            respDetail.put("output", toolResult.getOutput());
            result.setResponse(respDetail);
        } else {
            result.setStatus("ERROR");
            result.setMessage(toolResult != null ? toolResult.getError() : "工具方法执行失败");
        }
        result.setDurationMs(toolElapsed);

        return result;
    }

    /**
     * 执行 ACTION 类型关键字
     */
    private StepResult executeActionKeyword(StepNode step, Keyword keyword, ExecutionContext context) {
        Long refId = keyword.getRefId();
        if (refId == null) {
            return StepResult.error("ACTION 关键字缺少 refId：" + keyword.getName());
        }

        Action action = actionMapper.selectById(refId);
        if (action == null) {
            return StepResult.error("Action 不存在：" + refId);
        }

        // 委托 ActionExecutor 执行节点树
        ActionExecutor actionExecutor = SpringContextHolder.getBean(ActionExecutor.class);
        return actionExecutor.executeAction(action, step.getParams(), context);
    }

    @SuppressWarnings("unchecked")
    private Object extractByPath(Map<String, Object> map, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String cleanPath = path.startsWith("$.") ? path.substring(2)
                : (path.startsWith("$") ? path.substring(1) : path);
        String[] parts = cleanPath.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
