package org.example.imgroupserver.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 系统事件 RabbitMQ 配置
 * 生产者配置：声明交换机
 */
@Configuration
public class EventRabbitConfig {

    /**
     * 系统事件交换机名称
     */
    public static final String EVENT_EXCHANGE = "im.event.exchange";

    /**
     * 路由键
     */
    public static final String MEMBER_ADDED_ROUTING_KEY = "event.member.added";
    public static final String MEMBER_REMOVED_ROUTING_KEY = "event.member.removed";
    public static final String GROUP_DISBANDED_ROUTING_KEY = "event.group.disbanded";

    /**
     * 声明交换机
     */
    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter eventMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
