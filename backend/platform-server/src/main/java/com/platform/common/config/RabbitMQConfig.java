/**
 * @author HXN
 * @date 2026-08-18 16:20
 * @description RabbitMQ 配置类
 */
package com.platform.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 - 定义交换机、队列和绑定关系
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXECUTION_QUEUE = "execution.queue";
    public static final String EXECUTION_EXCHANGE = "execution.exchange";
    public static final String EXECUTION_ROUTING_KEY = "execution.routing";

    /**
     * 消息序列化 - 使用 Jackson JSON 替代默认 JDK 序列化
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue executionQueue() {
        return new Queue(EXECUTION_QUEUE, true);
    }

    @Bean
    public DirectExchange executionExchange() {
        return new DirectExchange(EXECUTION_EXCHANGE);
    }

    @Bean
    public Binding executionBinding(Queue executionQueue, DirectExchange executionExchange) {
        return BindingBuilder.bind(executionQueue).to(executionExchange).with(EXECUTION_ROUTING_KEY);
    }
}
