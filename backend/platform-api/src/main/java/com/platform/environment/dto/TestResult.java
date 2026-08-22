/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 环境测试结果 DTO
 */
package com.platform.environment.dto;

import lombok.Data;

/**
 * 环境连接测试结果
 */
@Data
public class TestResult {

    private Integer success;
    private String message;
    private Long responseTimeMs;

    public static TestResult ok(long responseTimeMs) {
        TestResult r = new TestResult();
        r.setSuccess(1);
        r.setMessage("连接成功");
        r.setResponseTimeMs(responseTimeMs);
        return r;
    }

    public static TestResult fail(String message) {
        TestResult r = new TestResult();
        r.setSuccess(0);
        r.setMessage(message);
        return r;
    }
}
