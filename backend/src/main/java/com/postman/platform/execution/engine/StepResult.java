package com.postman.platform.execution.engine;

import lombok.Data;

import java.util.Map;

/**
 * 单步骤执行结果
 */
@Data
public class StepResult {

    /**
     * 执行状态：PASSED / FAILED / SKIPPED / ERROR
     */
    private String status;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 请求详情（method / url / headers / body）
     */
    private Map<String, Object> request;

    /**
     * 响应详情（statusCode / headers / body / durationMs）
     */
    private Map<String, Object> response;

    /**
     * 断言结果摘要
     */
    private String assertionSummary;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 输出变量（供后续步骤引用）
     */
    private Map<String, Object> output;

    public static StepResult passed(String message) {
        StepResult r = new StepResult();
        r.setStatus("PASSED");
        r.setMessage(message);
        return r;
    }

    public static StepResult failed(String message) {
        StepResult r = new StepResult();
        r.setStatus("FAILED");
        r.setMessage(message);
        return r;
    }

    public static StepResult error(String message) {
        StepResult r = new StepResult();
        r.setStatus("ERROR");
        r.setMessage(message);
        return r;
    }
}
