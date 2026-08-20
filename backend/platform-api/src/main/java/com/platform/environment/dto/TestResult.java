package com.platform.environment.dto;

import lombok.Data;

/**
 * 环境连接测试结果
 */
@Data
public class TestResult {

    private Boolean success;
    private String message;
    private Long responseTimeMs;

    public static TestResult ok(long responseTimeMs) {
        TestResult r = new TestResult();
        r.setSuccess(true);
        r.setMessage("连接成功");
        r.setResponseTimeMs(responseTimeMs);
        return r;
    }

    public static TestResult fail(String message) {
        TestResult r = new TestResult();
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }
}
