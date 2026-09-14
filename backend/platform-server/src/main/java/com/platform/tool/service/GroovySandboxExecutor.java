/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Groovy 沙箱执行器
 */
package com.platform.tool.service;

import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.tool.dto.ToolTestResult;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Groovy 沙箱执行器
 * 安全执行用户提供的 Groovy 代码，带有白名单检查和超时控制
 */
@Component
@Slf4j
public class GroovySandboxExecutor {

    private static final long TIMEOUT_SECONDS = 30;

    /**
     * 形参声明中允许 as 转换的简单类型白名单
     *
     * <p>限定为 Groovy 内置类型，避免对未知类型做 as 转换导致脚本编译失败。
     */
    private static final Set<String> CASTABLE_TYPES = new HashSet<>(Arrays.asList(
            "int", "Integer", "long", "Long", "short", "Short", "byte", "Byte",
            "float", "Float", "double", "Double", "boolean", "Boolean",
            "char", "Character", "String", "CharSequence", "Object"
    ));

    /**
     * 危险操作黑名单关键字
     */
    private static final List<String> BLACKLIST = Arrays.asList(
            "Runtime.getRuntime",
            "ProcessBuilder",
            "System.exit",
            "java.io.File",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.io.FileWriter",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.net.URL",
            "java.lang.reflect",
            "javax.script.ScriptEngineManager",
            "GroovyShell",
            "GroovyClassLoader",
            "CompilerConfiguration",
            "Thread.currentThread",
            "System.setProperty",
            "System.getProperties"
    );

    /**
     * 执行 Groovy 代码
     *
     * @param code  Groovy 源代码
     * @param input 输入参数（JSON 字符串）
     * @return 执行结果
     */
    public ToolTestResult execute(String code, String input) {
        // 安全检查
        checkSecurity(code);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<ToolTestResult> future = executor.submit(() -> doExecute(code, input));
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Groovy 执行超时（{}s）", TIMEOUT_SECONDS);
            return ToolTestResult.fail("执行超时（" + TIMEOUT_SECONDS + "s）");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            String msg = cause != null ? cause.getMessage() : e.getMessage();
            log.warn("Groovy 执行异常", e);
            return ToolTestResult.fail("执行异常：" + msg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolTestResult.fail("执行被中断");
        } finally {
            executor.shutdownNow();
        }
    }

    private ToolTestResult doExecute(String code, String input) {
        long start = System.currentTimeMillis();

        CompilerConfiguration config = new CompilerConfiguration();
        Binding binding = new Binding();

        GroovyShell shell = new GroovyShell(binding, config);

        // 检测代码是否定义了函数，如果是则自动调用
        String codeToExecute = code;
        java.util.regex.Matcher funcMatcher = java.util.regex.Pattern
                .compile("(?:def|([A-Z]\\w*))\\s+(\\w+)\\s*\\(([\\s\\S]*?)\\)\\s*\\{")
                .matcher(code);
        if (funcMatcher.find()) {
            String funcName = funcMatcher.group(2);
            String paramsPart = funcMatcher.group(3).trim();
            if (paramsPart.isEmpty()) {
                // 无参函数：直接调用
                codeToExecute = code + "\n" + funcName + "()";
            } else {
                // 有参函数：解析 JSON 输入；对象输入（{"参数名": 值}）按参数名取值，数组输入按位置取值（兼容旧格式）
                String[] paramParts = paramsPart.split(",");
                StringBuilder argsList = new StringBuilder();
                for (int i = 0; i < paramParts.length; i++) {
                    if (i > 0) argsList.append(", ");
                    String paramName = extractParamName(paramParts[i]);
                    String expr;
                    if (paramName != null) {
                        expr = "__args instanceof Map ? __args.get('" + paramName + "') : __args[" + i + "]";
                    } else {
                        expr = "__args[" + i + "]";
                    }
                    // 按形参声明类型转换实参（前端测试值均为字符串，"12" 需转 int 等场景）
                    String paramType = extractParamType(paramParts[i]);
                    if (paramType != null) {
                        argsList.append("(").append(expr).append(") as ").append(paramType);
                    } else {
                        argsList.append(expr);
                    }
                }
                codeToExecute = code + "\ndef __args = new groovy.json.JsonSlurper().parseText(input)\n"
                        + funcName + "(" + argsList + ")";
            }
        }

        binding.setVariable("input", input != null ? input : "{}");
        Object result = shell.evaluate(codeToExecute);

        long elapsed = System.currentTimeMillis() - start;
        String output = result != null ? result.toString() : "null";
        return ToolTestResult.ok(output, elapsed);
    }

    /**
     * 从形参声明片段中解析参数名
     *
     * <p>兼容 "String prefix" / "int length" / "prefix" / "int length = 10" 等写法，
     * 取最后一段标识符；无法解析出合法标识符时返回 null（调用方回退为按位置取值）。
     */
    private String extractParamName(String paramPart) {
        String p = paramPart.trim();
        int eqIndex = p.indexOf('=');
        if (eqIndex >= 0) {
            p = p.substring(0, eqIndex).trim();
        }
        if (p.isEmpty()) {
            return null;
        }
        String[] segs = p.split("\\s+");
        String last = segs[segs.length - 1].trim();
        return last.matches("\\w+") ? last : null;
    }

    /**
     * 从形参声明片段中解析类型声明（仅限白名单内的简单类型）
     *
     * <p>"int length" → "int"；"length" / "int length = 10"（剥离默认值后无类型）→ null。
     * 泛型等复杂声明（"Map&lt;String, Object&gt; m"）与未知类型返回 null，
     * 不做 as 转换（未知类型 as 转换会导致脚本编译失败）。
     */
    private String extractParamType(String paramPart) {
        String p = paramPart.trim();
        int eqIndex = p.indexOf('=');
        if (eqIndex >= 0) {
            p = p.substring(0, eqIndex).trim();
        }
        String[] segs = p.split("\\s+");
        if (segs.length < 2) {
            return null;
        }
        String type = segs[0].trim();
        return CASTABLE_TYPES.contains(type) ? type : null;
    }

    /**
     * 安全检查：扫描代码中是否包含黑名单关键字
     */
    private void checkSecurity(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.TOOL_SECURITY_CHECK_FAILED, "代码不能为空");
        }

        for (String keyword : BLACKLIST) {
            if (code.contains(keyword)) {
                throw new BusinessException(ErrorCode.TOOL_SECURITY_CHECK_FAILED,
                        "安全检查失败：代码中包含禁止使用的操作 [" + keyword + "]");
            }
        }

        // 检查是否存在 import 语句引入危险包
        String[] lines = code.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                if (trimmed.contains("java.io") || trimmed.contains("java.net")
                        || trimmed.contains("java.lang.reflect")
                        || trimmed.contains("groovy.lang.GroovyShell")
                        || trimmed.contains("groovy.lang.GroovyClassLoader")) {
                    throw new BusinessException(ErrorCode.TOOL_SECURITY_CHECK_FAILED,
                            "安全检查失败：禁止导入危险包 [" + trimmed + "]");
                }
            }
        }
    }
}
