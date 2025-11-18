package org.example.mpushservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 系统事件 RabbitMQ 配置
 * 消费者配置：声明交换机、队列和绑定关系
 */
@Configuration
public class EventRabbitConfig {

    /**
     * 系统事件交换机名称（与生产者保持一致）
     */
    public static final String EVENT_EXCHANGE = "im.event.exchange";

    /**
     * 队列名称
     */
    public static final String EVENT_QUEUE = "im.event.queue";

    /**
     * 路由键模式（订阅所有 event.* 的消息）
     */
    public static final String EVENT_ROUTING_PATTERN = "event.*";

    /**
     * 声明交换机
     */
    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    /**
     * 声明队列
     */
    @Bean
    public Queue eventQueue() {
        return new Queue(EVENT_QUEUE, true);
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding eventBinding(Queue eventQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(eventQueue).to(eventExchange).with(EVENT_ROUTING_PATTERN);
    }

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter eventMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
