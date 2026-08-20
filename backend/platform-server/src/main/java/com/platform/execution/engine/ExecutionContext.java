package com.platform.execution.engine;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 执行上下文
 *
 * <p>贯穿单次执行的生命周期，持有变量、环境配置和日志收集器。
 * 变量支持 ${var} 语法引用，用于参数化步骤。
 */
@Data
public class ExecutionContext {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private Long executionId;

    private Long projectId;

    private Long environmentId;

    /**
     * 从环境配置解析的 baseUrl（如 http://localhost:8080）
     */
    private String baseUrl;

    /**
     * 环境额外配置（从 Environment.configJson 解析）
     */
    private Map<String, Object> envConfig = new HashMap<>();

    /**
     * 执行变量（步骤间共享）
     */
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 执行日志（每步骤的 req/res 详情）
     */
    private List<Map<String, Object>> logs = new ArrayList<>();

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 变量替换：将 ${var} 替换为实际值
     */
    public String resolveVariables(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = VAR_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object val = variables.get(key);
            matcher.appendReplacement(sb, val != null ? Matcher.quoteReplacement(val.toString()) : "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 添加执行日志
     */
    public void addLog(Map<String, Object> log) {
        logs.add(log);
    }
}
