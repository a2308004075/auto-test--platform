/**
 * @author HXN
 * @date 2026-08-28 10:00
 * @description $ref{参数名} 统一参数接收语法单元测试
 */
package com.platform.execution.engine;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * KeywordExecutor.resolveRefs 单元测试
 *
 * <p>验证统一参数接收语法 $ref{参数名} 的替换行为。
 */
class KeywordExecutorResolveRefsTest {

    @Test
    void replacesSingleRef() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "admin");

        assertEquals("admin", KeywordExecutor.resolveRefs("$ref{username}", params));
    }

    @Test
    void replacesMultipleRefsInBody() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "admin");
        params.put("password", "123456");

        assertEquals("{\"username\":\"admin\",\"password\":\"123456\"}",
                KeywordExecutor.resolveRefs("{\"username\":\"$ref{username}\",\"password\":\"$ref{password}\"}", params));
    }

    @Test
    void unpassedParamReplacedWithEmptyString() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "admin");

        assertEquals("admin:", KeywordExecutor.resolveRefs("$ref{username}:$ref{token}", params));
    }

    @Test
    void keepsDollarVarPlaceholdersUntouched() {
        Map<String, Object> params = new HashMap<>();
        params.put("host", "example.com");

        // ${var} 上下文变量占位符不参与 $ref 替换，由 HttpClientEngine 后续处理；$ref 正常替换
        assertEquals("${baseUrl}/example.com", KeywordExecutor.resolveRefs("${baseUrl}/$ref{host}", params));
    }

    @Test
    void nullOrEmptyInputsReturnAsIs() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "admin");

        assertEquals(null, KeywordExecutor.resolveRefs(null, params));
        assertEquals("", KeywordExecutor.resolveRefs("", params));
        assertEquals("no refs", KeywordExecutor.resolveRefs("no refs", null));
    }

    @Test
    void replacementValueEscaping() {
        Map<String, Object> params = new HashMap<>();
        params.put("expr", "a${b}c");

        // 实参值含 ${} 字面量时不破坏替换结果
        assertEquals("a${b}c", KeywordExecutor.resolveRefs("$ref{expr}", params));
    }
}
