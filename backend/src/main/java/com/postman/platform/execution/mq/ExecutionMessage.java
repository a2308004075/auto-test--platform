package com.postman.platform.execution.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 执行消息体（MQ 传输）
 */
@Data
public class ExecutionMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long executionId;

    private Long planId;

    private Long environmentId;

    private Long triggeredBy;

    private String triggerType;
}
