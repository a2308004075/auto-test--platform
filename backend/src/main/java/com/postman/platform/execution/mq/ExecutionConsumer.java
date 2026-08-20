package com.postman.platform.execution.mq;

import com.postman.platform.execution.engine.PlanExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 执行消息消费者
 *
 * <p>从 execution.queue 消费执行请求，调用 PlanExecutor 执行测试计划。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionConsumer {

    private final PlanExecutor planExecutor;

    @RabbitListener(queues = "execution.queue")
    public void handleExecution(ExecutionMessage message) {
        log.info("收到执行消息: executionId={}, planId={}", message.getExecutionId(), message.getPlanId());
        try {
            planExecutor.execute(message.getExecutionId());
        } catch (Exception e) {
            log.error("执行消息处理异常: executionId={}", message.getExecutionId(), e);
        }
    }
}
