package com.example.user.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户服务 rabbitmq 配置 主要是用于用户注册时发送消息
 */
@Configuration
public class UserServiceRabbitConfig {

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 定义user交换机名称 此交换机用与用户注册时发送消息
     */
    public static final String EXCHANGE_NAME = "user.exchange";

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 消息转换器 (Message Converter)
     * 用来将发送的 Java 对象转换为 JSON。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 声明交换机
     * 生产者声明交换机,保证无论是先启动生产者还是先启动消费者,交换机都存在
     */
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }
}