package com.postman.platform.execution.mq;

import com.postman.platform.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 执行消息生产者
 *
 * <p>将执行请求异步发送到 RabbitMQ execution.queue。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送执行消息
     */
    public void sendExecutionMessage(ExecutionMessage message) {
        log.info("发送执行消息: executionId={}, planId={}", message.getExecutionId(), message.getPlanId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXECUTION_EXCHANGE,
                RabbitMQConfig.EXECUTION_ROUTING_KEY,
                message
        );
    }
}
