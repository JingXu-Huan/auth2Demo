package com.example.email.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailServiceRabbitConfig {

    // 用户注册确认邮件
    public static final String EXCHANGE_NAME = "user.exchange";
    public static final String QUEUE_NAME = "email.confirmation.queue";
    public static final String ROUTING_KEY = "user.registration.confirm";
    
    // 安全验证码邮件
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_QUEUE = "email.send.queue";
    public static final String EMAIL_ROUTING_KEY = "email.send";

    /**
     * 1. 声明交换机 (TopicExchange)
     * 我们使用 Topic Exchange，因为它更灵活，
     * 以后可以扩展用于 "user.password.reset" 等。
     */
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * 2. 声明队列 (Queue)
     * 队列用于存储“发送确认邮件”的任务
     */
    @Bean
    public Queue confirmationQueue() {
        // new Queue(name, durable)
        // durable = true (默认) 意味着队列在 RabbitMQ 重启后仍然存在
        return new Queue(QUEUE_NAME, true);
    }

    /**
     * 3. 声明绑定 (Binding)
     * 将交换机和队列通过路由键 (Routing Key) 绑定在一起。
     * 所有发送到 user.exchange 且路由键为 user.registration.confirm 的消息
     * 都会被路由到 email.confirmation.queue 队列。
     */
    @Bean
    public Binding binding(Queue confirmationQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(confirmationQueue)
                .to(userExchange)
                .with(ROUTING_KEY);
    }

    /**
     * 4. 消息转换器 (Message Converter)
     * 我们使用 JSON 格式来序列化和反序列化消息。
     * Spring Boot 会自动使用这个 Bean 来处理消息。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    //mail exchange
    
    /**
     * 5. 声明邮件交换机
     */
    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE);
    }
    
    /**
     * 6. 声明邮件队列
     */
    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }
    
    /**
     * 7. 绑定邮件队列到交换机
     */
    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(emailExchange)
                .with(EMAIL_ROUTING_KEY);
    }
}