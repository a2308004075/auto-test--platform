/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 执行消息消费者
 */
package com.platform.execution.mq;

import com.platform.execution.engine.PlanExecutor;
import com.platform.execution.entity.TestExecution;
import com.platform.execution.mapper.TestExecutionMapper;
import com.platform.execution.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 执行消息消费者
 *
 * <p>从 execution.queue 消费执行请求，调用 PlanExecutor 执行测试计划。
 * 执行完成后（无论成功或失败）自动触发下一个排队任务。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionConsumer {

    private final PlanExecutor planExecutor;
    private final ExecutionService executionService;
    private final TestExecutionMapper testExecutionMapper;

    @RabbitListener(queues = "execution.queue")
    public void handleExecution(ExecutionMessage message) {
        log.info("收到执行消息: executionId={}, planId={}", message.getExecutionId(), message.getPlanId());
        Long executionId = message.getExecutionId();

        try {
            // 检查执行是否已被取消
            TestExecution execution = testExecutionMapper.selectById(executionId);
            if (execution == null) {
                log.warn("执行记录不存在，跳过: executionId={}", executionId);
                return;
            }
            if ("CANCELLED".equals(execution.getStatus())) {
                log.info("执行已被取消，跳过: executionId={}", executionId);
                triggerNextQueued();
                return;
            }

            // 调用 PlanExecutor 执行
            planExecutor.execute(executionId);

        } catch (Throwable e) {
            // 刻意捕获 Throwable：Error 场景（如 StackOverflowError）也要标记执行失败并正常 ACK，
            // 避免异常逃逸导致消息 requeue 死循环、阻塞后续排队任务
            log.error("执行消息处理异常: executionId={}", executionId, e);
            // 标记为 ERROR
            markError(executionId, e.getMessage());
        } finally {
            // 无论成功或失败，触发下一个排队任务
            triggerNextQueued();
        }
    }

    /**
     * 标记执行为 ERROR 状态
     */
    private void markError(Long executionId, String errorMessage) {
        try {
            TestExecution execution = testExecutionMapper.selectById(executionId);
            if (execution != null && !"COMPLETED".equals(execution.getStatus())
                    && !"CANCELLED".equals(execution.getStatus())) {
                execution.setStatus("FAILED");
                execution.setFinishedAt(LocalDateTime.now());
                testExecutionMapper.updateById(execution);
            }
        } catch (Exception ex) {
            log.error("标记 ERROR 状态失败: executionId={}", executionId, ex);
        }
    }

    /**
     * 触发下一个排队任务
     */
    private void triggerNextQueued() {
        try {
            executionService.triggerNextQueued();
        } catch (Exception e) {
            log.warn("触发排队任务失败: {}", e.getMessage());
        }
    }
}
