/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 关键字执行器
 */
package com.platform.execution.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.action.entity.Action;
import com.platform.action.mapper.ActionMapper;
import com.platform.apidoc.entity.Api;
import com.platform.apidoc.mapper.ApiMapper;
import com.platform.apidoc.service.ApiModuleService;
import com.platform.common.util.SpringContextHolder;
import com.platform.project.entity.ApiModule;
import com.platform.execution.entity.AutoCase;
import com.platform.execution.mapper.AutoCaseMapper;
import com.platform.keyword.entity.ApiKeyword;
import com.platform.keyword.entity.Keyword;
import com.platform.keyword.mapper.ApiKeywordMapper;
import com.platform.keyword.mapper.KeywordMapper;
import com.platform.tool.entity.ToolMethod;
import com.platform.tool.mapper.ToolMethodMapper;
import com.platform.tool.dto.ToolTestResult;
import com.platform.tool.service.GroovySandboxExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键字执行器
 *
 * <p>统一入口，根据关键字类型分发执行：
 * <ul>
 *   <li>API → HttpClientEngine（查 ApiKeyword → Api → 发送 HTTP 请求）</li>
 *   <li>TOOL → GroovySandboxExecutor（查 ToolMethod → 执行 Groovy 代码）</li>
 *   <li>ACTION → ActionExecutor（查 Action → 执行节点树）</li>
 *   <li>AUTO_CASE → AutoCaseExecutor（查 AutoCase → 执行嵌套自动化用例）</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeywordExecutor {

    /**
     * $ref{参数名} 占位符：接口关键字内声明的参数接收点，
     * 执行时用引用方（Action/自动化用例）传入的同名实参替换
     */
    private static final Pattern REF_PATTERN = Pattern.compile("\\$ref\\{([^}]+)}");

    private final KeywordMapper keywordMapper;
    private final ApiKeywordMapper apiKeywordMapper;
    private final ApiMapper apiMapper;
    private final ApiModuleService apiModuleService;
    private final ToolMethodMapper toolMethodMapper;
    private final ActionMapper actionMapper;
    private final AutoCaseMapper autoCaseMapper;
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
            case "AUTO_CASE":
                return executeAutoCaseKeyword(step, keyword, context);
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

        // 解析接口所在分组的有效服务前缀（子分组优先）
        Map<Long, ApiModule> moduleMap = apiModuleService.getModuleMap(api.getProjectId());
        String servicePrefix = apiModuleService.resolveServicePrefix(api.getModuleId(), moduleMap);

        // 解析 headers（Api.headers 是 JSON 数组字符串）
        Map<String, String> headers = new LinkedHashMap<>();
        if (api.getHeaders() != null && !api.getHeaders().isEmpty()) {
            try {
                List<Map<String, Object>> apiHeaders = objectMapper.readValue(api.getHeaders(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> h : apiHeaders) {
                    String name = (String) h.get("name");
                    if (name != null) {
                        Object value = h.get("value");
                        headers.put(name, value != null ? value.toString() : "");
                    }
                }
            } catch (Exception e) {
                log.warn("解析接口 headers 失败: {}", e.getMessage());
            }
        }

        // 解析关键字级缺省实参（testData 中的预设值）与关键字级请求体模板（__body__ 行）
        Map<String, Object> defaultParams = new LinkedHashMap<>();
        String keywordBodyTemplate = null;
        if (apiKeyword.getTestData() != null && !apiKeyword.getTestData().isEmpty()) {
            try {
                List<Map<String, Object>> rows = objectMapper.readValue(apiKeyword.getTestData(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> row : rows) {
                    String name = row.get("name") != null ? row.get("name").toString() : null;
                    Object value = row.get("value");
                    if ("__body__".equals(name)) {
                        keywordBodyTemplate = value != null ? value.toString() : null;
                    } else if (name != null && !name.isEmpty()) {
                        defaultParams.put(name, value);
                    }
                }
            } catch (Exception e) {
                log.warn("解析关键字 testData 失败: {}", e.getMessage());
            }
        }

        // 请求体：优先使用关键字级 __body__ 模板，否则回退接口定义的 requestBody
        String body = keywordBodyTemplate != null && !keywordBodyTemplate.isEmpty()
                ? keywordBodyTemplate
                : (api.getRequestBody() != null && !api.getRequestBody().isEmpty() ? api.getRequestBody() : null);

        // 合并缺省实参与引用方实参（引用方覆盖）
        Map<String, Object> refParams = new LinkedHashMap<>(defaultParams);
        if (step.getParams() != null) {
            refParams.putAll(step.getParams());
        }

        // 前导 ${var} host 占位符保持在最前，不参与路径参数替换（替代 baseUrl 的 host）
        String path = api.getPath();
        String leadingPlaceholder = "";
        if (path.startsWith("${")) {
            int end = path.indexOf('}');
            if (end > 0) {
                leadingPlaceholder = path.substring(0, end + 1);
                path = path.substring(end + 1);
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
            }
        }

        // 路径参数替换：路径中的 {参数名} 占位符（REST 惯例，接口定义固有）按名接收实参
        for (Map.Entry<String, Object> entry : refParams.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (path.contains(placeholder) && entry.getValue() != null) {
                path = path.replace(placeholder, entry.getValue().toString());
            }
        }

        // 拼接服务前缀与路径
        String fullPath = leadingPlaceholder + joinPath(servicePrefix, path);

        // $ref{参数名} 替换：请求头值与请求体中声明的接收点，用引用方实参（优先）或关键字缺省实参填充
        headers.replaceAll((k, v) -> resolveRefs(v, refParams));
        body = resolveRefs(body, refParams);

        // 发送 HTTP 请求
        StepResult result = httpClientEngine.execute(
                api.getHttpMethod(), fullPath, headers, body, context);

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
        if (toolResult != null && Integer.valueOf(1).equals(toolResult.getSuccess())) {
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

    /**
     * 执行 AUTO_CASE 类型关键字
     *
     * <p>查 AutoCase 实体，委托 AutoCaseExecutor 执行嵌套自动化用例。
     * 使用 SpringContextHolder 获取 AutoCaseExecutor 避免循环依赖。
     */
    private StepResult executeAutoCaseKeyword(StepNode step, Keyword keyword, ExecutionContext context) {
        Long refId = keyword.getRefId();
        if (refId == null) {
            return StepResult.error("AUTO_CASE 关键字缺少 refId：" + keyword.getName());
        }

        AutoCase autoCase = autoCaseMapper.selectById(refId);
        if (autoCase == null) {
            return StepResult.error("自动化用例不存在：" + refId);
        }

        // 使用 SpringContextHolder 获取 AutoCaseExecutor 避免循环依赖
        AutoCaseExecutor autoCaseExecutor = SpringContextHolder.getBean(AutoCaseExecutor.class);
        return autoCaseExecutor.execute(autoCase, context);
    }

    /**
     * $ref{参数名} 替换：用引用方传入的同名实参替换关键字内声明的参数接收点
     *
     * <p>统一参数接收语法：接口关键字的请求头值、请求体中写 $ref{参数名}，
     * 执行时以 step.params（引用方实参）按名替换；未传入的参数替换为空串。
     */
    static String resolveRefs(String text, Map<String, Object> params) {
        if (text == null || text.isEmpty() || params == null || params.isEmpty()) {
            return text;
        }
        Matcher matcher = REF_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object val = params.get(key);
            matcher.appendReplacement(sb, val != null ? Matcher.quoteReplacement(val.toString()) : "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String joinPath(String prefix, String path) {
        if (!org.springframework.util.StringUtils.hasText(prefix)) {
            return path == null ? "" : path;
        }
        if (path == null || path.isEmpty()) {
            return prefix;
        }
        String p = prefix;
        if (p.endsWith("/") && path.startsWith("/")) {
            p = p.substring(0, p.length() - 1);
        } else if (!p.endsWith("/") && !path.startsWith("/")) {
            p = p + "/";
        }
        return p + path;
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
