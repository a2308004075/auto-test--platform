/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 断言引擎
 */
package com.platform.execution.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 断言引擎
 *
 * <p>根据断言列表验证关键字执行结果，支持状态码、响应体、JSONPath、Header 等断言类型。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssertionEngine {

    private final ObjectMapper objectMapper;

    /**
     * 执行断言列表
     *
     * @param assertions 断言列表
     * @param response   响应详情（statusCode / headers / body）
     * @return 断言结果摘要（通过数/总数 + 失败详情）
     */
    public String evaluate(List<AssertionItem> assertions, Map<String, Object> response) {
        if (assertions == null || assertions.isEmpty()) {
            return null;
        }

        int passed = 0;
        int total = assertions.size();
        StringBuilder failures = new StringBuilder();

        for (AssertionItem assertion : assertions) {
            boolean result = evaluateSingle(assertion, response);
            if (result) {
                passed++;
            } else {
                if (failures.length() > 0) {
                    failures.append("; ");
                }
                failures.append(String.format("[%s %s %s] 期望=%s",
                        assertion.getType(),
                        assertion.getField() != null ? assertion.getField() : "",
                        assertion.getOperator(),
                        assertion.getExpected()));
            }
        }

        if (passed == total) {
            return String.format("断言全部通过 (%d/%d)", passed, total);
        } else {
            return String.format("断言部分通过 (%d/%d)，失败: %s", passed, total, failures);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateSingle(AssertionItem assertion, Map<String, Object> response) {
        try {
            String type = assertion.getType();
            String operator = assertion.getOperator();
            Object expected = assertion.getExpected();

            switch (type != null ? type : "") {
                case "STATUS_CODE":
                    int statusCode = response.get("statusCode") != null
                            ? Integer.parseInt(response.get("statusCode").toString()) : 0;
                    return compare(String.valueOf(statusCode), operator, expected != null ? expected.toString() : null);

                case "RESPONSE_BODY":
                    Object respBody = response.get("body");
                    if (respBody == null) {
                        return "not_exists".equals(operator);
                    }
                    return compare(respBody.toString(), operator, expected != null ? expected.toString() : null);

                case "JSON_PATH":
                    // 简化版 JSONPath：使用字段名从响应体提取值
                    Object body = response.get("body");
                    if (body == null) {
                        return false;
                    }
                    Map<String, Object> bodyMap = objectMapper.readValue(body.toString(), Map.class);
                    Object actual = extractByPath(bodyMap, assertion.getField());
                    return compareObject(actual, operator, expected);

                case "HEADER":
                    Map<String, Object> headers = (Map<String, Object>) response.get("headers");
                    if (headers == null) {
                        return false;
                    }
                    Object headerVal = headers.get(assertion.getField());
                    if (headerVal == null) {
                        return "not_exists".equals(operator);
                    }
                    return compare(headerVal.toString(), operator, expected != null ? expected.toString() : null);

                case "RESPONSE_TIME":
                    Object duration = response.get("durationMs");
                    if (duration == null) {
                        return false;
                    }
                    long actualMs = Long.parseLong(duration.toString());
                    long expectedMs = expected != null ? Long.parseLong(expected.toString()) : 0;
                    return compareNumeric(actualMs, operator, expectedMs);

                default:
                    log.warn("未知断言类型: {}", type);
                    return false;
            }
        } catch (Exception e) {
            log.warn("断言执行异常: {}", e.getMessage());
            return false;
        }
    }

    private boolean compare(String actual, String operator, String expected) {
        if (actual == null) actual = "";
        if (expected == null) expected = "";
        switch (operator != null ? operator : "eq") {
            case "eq": return actual.equals(expected);
            case "ne": return !actual.equals(expected);
            case "contains": return actual.contains(expected);
            case "not_contains": return !actual.contains(expected);
            case "exists": return true;
            case "not_exists": return actual.isEmpty();
            default: return false;
        }
    }

    private boolean compareObject(Object actual, String operator, Object expected) {
        if (actual == null) {
            return "not_exists".equals(operator);
        }
        return compare(actual.toString(), operator, expected != null ? expected.toString() : null);
    }

    private boolean compareNumeric(long actual, String operator, long expected) {
        switch (operator != null ? operator : "le") {
            case "eq": return actual == expected;
            case "ne": return actual != expected;
            case "gt": return actual > expected;
            case "lt": return actual < expected;
            case "ge": return actual >= expected;
            case "le": return actual <= expected;
            default: return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Object extractByPath(Map<String, Object> map, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        // 去除前导 $. 或 $
        String cleanPath = path.startsWith("$.") ? path.substring(2) : (path.startsWith("$") ? path.substring(1) : path);
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
