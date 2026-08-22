/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 执行消息体
 */
package com.platform.execution.mq;

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
