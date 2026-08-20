package com.postman.platform.execution.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 执行消息体（MQ 传输）
 */
@Data
public class ExecutionMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String executionId;

    private String planId;

    private String environmentId;

    private String triggeredBy;

    private String triggerType;
}
