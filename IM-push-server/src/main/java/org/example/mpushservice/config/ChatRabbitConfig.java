package org.example.mpushservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天消息 RabbitMQ 配置
 * IM-push-server 作为消费者，接收聊天消息并推送
 */
@Configuration
public class ChatRabbitConfig {

    /**
     * 聊天消息交换机（与 IM-message-server 保持一致）
     */
    public static final String CHAT_EXCHANGE = "im.chat.exchange";

    /**
     * 队列名称
     */
    public static final String CHAT_QUEUE = "im.chat.queue";

    /**
     * 路由键模式（订阅所有 chat.* 的消息）
     */
    public static final String CHAT_ROUTING_PATTERN = "chat.*";

    /**
     * 声明交换机
     */
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    /**
     * 声明队列
     */
    @Bean
    public Queue chatQueue() {
        return new Queue(CHAT_QUEUE, true);
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding chatBinding(Queue chatQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatQueue).to(chatExchange).with(CHAT_ROUTING_PATTERN);
    }

    /**
     * 专用于聊天消息监听的容器工厂：使用 SimpleMessageConverter，直接将消息体作为 byte[] 传入监听方法
     */
    @Bean(name = "rawRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rawRabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        return factory;
    }
}
