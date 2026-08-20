package com.postman.platform.execution.dto;

import lombok.Data;

/**
 * 触发执行请求
 */
@Data
public class ExecutionStartRequest {

    /**
     * 执行环境 ID（可选，为空则使用计划默认环境）
     */
    private String environmentId;

    /**
     * 触发方式：MANUAL / SCHEDULED / CI
     */
    private String triggerType;
}
