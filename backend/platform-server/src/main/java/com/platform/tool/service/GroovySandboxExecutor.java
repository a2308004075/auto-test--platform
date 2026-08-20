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
import java.util.List;
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
        binding.setVariable("input", input);

        GroovyShell shell = new GroovyShell(binding, config);
        Object result = shell.evaluate(code);

        long elapsed = System.currentTimeMillis() - start;
        String output = result != null ? result.toString() : "null";
        return ToolTestResult.ok(output, elapsed);
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
