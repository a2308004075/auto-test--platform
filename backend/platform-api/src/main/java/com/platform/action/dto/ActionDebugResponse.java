package com.platform.action.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ActionDebugResponse {

    private Boolean success;
    private Map<String, Object> output;
    private List<Map<String, Object>> nodeResults;
    private Long executionTimeMs;
    private String errorMessage;

    public static ActionDebugResponse ok(Map<String, Object> output,
                                          List<Map<String, Object>> nodeResults,
                                          long executionTimeMs) {
        ActionDebugResponse r = new ActionDebugResponse();
        r.setSuccess(true);
        r.setOutput(output);
        r.setNodeResults(nodeResults);
        r.setExecutionTimeMs(executionTimeMs);
        return r;
    }

    public static ActionDebugResponse fail(String errorMessage) {
        ActionDebugResponse r = new ActionDebugResponse();
        r.setSuccess(false);
        r.setErrorMessage(errorMessage);
        return r;
    }
}
