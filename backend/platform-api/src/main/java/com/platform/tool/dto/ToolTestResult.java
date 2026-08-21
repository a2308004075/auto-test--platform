package com.platform.tool.dto;

import lombok.Data;

@Data
public class ToolTestResult {

    private Integer success;
    private String output;
    private String error;
    private Long executionTimeMs;

    public static ToolTestResult ok(String output, long executionTimeMs) {
        ToolTestResult r = new ToolTestResult();
        r.setSuccess(1);
        r.setOutput(output);
        r.setExecutionTimeMs(executionTimeMs);
        return r;
    }

    public static ToolTestResult fail(String error) {
        ToolTestResult r = new ToolTestResult();
        r.setSuccess(0);
        r.setError(error);
        return r;
    }
}
