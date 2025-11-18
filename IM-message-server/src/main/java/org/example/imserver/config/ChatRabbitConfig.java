package org.example.imserver.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天消息 RabbitMQ 配置
 * IM-message-server 作为生产者，发送聊天消息到 MQ
 */
@Configuration
public class ChatRabbitConfig {

    /**
     * 聊天消息交换机
     */
    public static final String CHAT_EXCHANGE = "im.chat.exchange";

    /**
     * 路由键
     */
    public static final String PRIVATE_MESSAGE_ROUTING_KEY = "chat.private";
    public static final String GROUP_MESSAGE_ROUTING_KEY = "chat.group";

    /**
     * 声明交换机
     */
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter chatMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
