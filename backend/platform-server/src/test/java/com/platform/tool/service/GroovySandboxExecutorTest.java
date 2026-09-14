/**
 * @author HXN
 * @date 2026-08-30
 * @description Groovy 沙箱执行器函数式调用传参单元测试
 */
package com.platform.tool.service;

import com.platform.tool.dto.ToolTestResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GroovySandboxExecutor 单元测试
 *
 * <p>重点验证函数式调用的实参传递：
 * <ul>
 *   <li>对象输入（{"参数名": 值}）→ 按参数名取值（前端在线测试 / Action 节点 / 用例步骤的实际传参格式）</li>
 *   <li>数组输入（[值1, 值2]）→ 按位置取值（兼容旧格式）</li>
 * </ul>
 */
class GroovySandboxExecutorTest {

    private final GroovySandboxExecutor executor = new GroovySandboxExecutor();

    @Test
    void functionCall_withObjectInput_resolvesByParamName() {
        ToolTestResult result = executor.execute(
                "def add(int a, int b) {\n    return a + b\n}",
                "{\"a\": 1, \"b\": 2}");

        assertEquals(Integer.valueOf(1), result.getSuccess(),
                "对象输入应按参数名取值执行成功: " + result.getError());
        assertEquals("3", result.getOutput());
    }

    @Test
    void functionCall_withObjectInput_stringNumbers() {
        // 前端 el-input 绑定的测试值均为字符串，字符串数字应可传给 int 形参
        ToolTestResult result = executor.execute(
                "def add(int a, int b) {\n    return a + b\n}",
                "{\"a\": \"1\", \"b\": \"2\"}");

        assertEquals(Integer.valueOf(1), result.getSuccess(),
                "字符串数字实参应执行成功: " + result.getError());
        assertEquals("3", result.getOutput());
    }

    @Test
    void functionCall_withObjectInput_typedParams() {
        // 与前端"插入模板"一致的场景：类型化形参 + 对象输入
        ToolTestResult result = executor.execute(
                "def generate_sn(String prefix, int length) {\n"
                        + "    def chars = \"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\"\n"
                        + "    def random = new Random()\n"
                        + "    def sb = new StringBuilder()\n"
                        + "    def remaining = length - prefix.length()\n"
                        + "    for (int i = 0; i < remaining; i++) {\n"
                        + "        sb.append(chars.charAt(random.nextInt(chars.length())))\n"
                        + "    }\n"
                        + "    return prefix + sb.toString()\n"
                        + "}",
                "{\"prefix\": \"BAT\", \"length\": \"12\"}");

        assertEquals(Integer.valueOf(1), result.getSuccess(),
                "类型化形参应按参数名取值执行成功: " + result.getError());
        assertTrue(result.getOutput().startsWith("BAT"), "返回值应以前缀开头: " + result.getOutput());
        assertEquals(12, result.getOutput().length(), "返回值长度应为 12: " + result.getOutput());
    }

    @Test
    void functionCall_withArrayInput_resolvesByPosition() {
        ToolTestResult result = executor.execute(
                "def add(int a, int b) {\n    return a + b\n}",
                "[1, 2]");

        assertEquals(Integer.valueOf(1), result.getSuccess(),
                "数组输入应按位置取值执行成功: " + result.getError());
        assertEquals("3", result.getOutput());
    }

    @Test
    void functionCall_withUntypedParams_objectInput() {
        // 无类型形参：按参数名取值，不做类型转换
        ToolTestResult result = executor.execute(
                "def join(prefix, suffix) {\n    return prefix + \"-\" + suffix\n}",
                "{\"prefix\": \"a\", \"suffix\": \"b\"}");

        assertEquals(Integer.valueOf(1), result.getSuccess(),
                "无类型形参应按参数名取值执行成功: " + result.getError());
        assertEquals("a-b", result.getOutput());
    }

    @Test
    void functionCall_objectInput_missingNumericParam_failsWithClearError() {
        // 对象输入缺必填参数：null as int 抛 GroovyCastException，明确报错优于静默降级为 0
        ToolTestResult result = executor.execute(
                "def add(int a, int b) {\n    return a + b\n}",
                "{\"a\": 1}");

        assertEquals(Integer.valueOf(0), result.getSuccess(), "缺必填参数应执行失败");
        assertTrue(result.getError() != null && result.getError().contains("int"),
                "错误信息应指明 int 类型转换失败: " + result.getError());
    }

    @Test
    void functionCall_withDefaultParam_objectInput() {
        // 带默认值的形参：参数名解析需去掉 "= 默认值" 部分
        ToolTestResult result = executor.execute(
                "def greet(String name, String greeting = \"Hello\") {\n    return greeting + \", \" + name\n}",
                "{\"name\": \"Tom\", \"greeting\": \"Hi\"}");

        assertEquals(Integer.valueOf(1), result.getSuccess(),
                "带默认值形参应按参数名取值执行成功: " + result.getError());
        assertEquals("Hi, Tom", result.getOutput());
    }

    @Test
    void noArgFunction_directCall() {
        ToolTestResult result = executor.execute("def hello() {\n    return \"hi\"\n}", "{}");

        assertEquals(Integer.valueOf(1), result.getSuccess());
        assertEquals("hi", result.getOutput());
    }

    @Test
    void scriptMode_inputVariableBinding() {
        // 非函数脚本模式：内置变量 input（JSON 字符串）保持可用
        ToolTestResult result = executor.execute(
                "def p = new groovy.json.JsonSlurper().parseText(input)\nreturn p.a + p.b",
                "{\"a\": 1, \"b\": 2}");

        assertEquals(Integer.valueOf(1), result.getSuccess());
        assertEquals("3", result.getOutput());
    }
}
