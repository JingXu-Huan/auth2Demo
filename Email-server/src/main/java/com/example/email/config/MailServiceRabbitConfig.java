package com.example.email.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 使用交换机模式，通过路由键绑定队列
 */
@Configuration
public class MailServiceRabbitConfig {

    /**
     * 交换机名称（与User-server保持一致）
     */
    public static final String EXCHANGE_NAME = "user.exchange";
    
    /**
     * 队列名称
     */
    public static final String VERIFICATION_QUEUE = "email.verification.queue";
    public static final String WELCOME_QUEUE = "email.welcome.queue";
    public static final String CONFIRMATION_QUEUE = "email.confirmation.queue";
    
    /**
     * 路由键
     */
    public static final String VERIFICATION_ROUTING_KEY = "email.verification";
    public static final String WELCOME_ROUTING_KEY = "email.welcome";
    public static final String CONFIRMATION_ROUTING_KEY = "email.confirmation";

    /**
     * 声明交换机
     * 消费者也声明交换机，保证无论启动顺序如何，交换机都存在
     */
    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * 邮箱验证队列
     */
    @Bean
    public Queue verificationQueue() {
        return new Queue(VERIFICATION_QUEUE, true);
    }
    
    /**
     * 欢迎邮件队列
     */
    @Bean
    public Queue welcomeQueue() {
        return new Queue(WELCOME_QUEUE, true);
    }
    
    /**
     * 确认邮件队列
     */
    @Bean
    public Queue confirmationQueue() {
        return new Queue(CONFIRMATION_QUEUE, true);
    }

    /**
     * 绑定验证邮件队列到交换机
     */
    @Bean
    public Binding verificationBinding(Queue verificationQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(verificationQueue).to(emailExchange).with(VERIFICATION_ROUTING_KEY);
    }
    
    /**
     * 绑定欢迎邮件队列到交换机
     */
    @Bean
    public Binding welcomeBinding(Queue welcomeQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(welcomeQueue).to(emailExchange).with(WELCOME_ROUTING_KEY);
    }
    
    /**
     * 绑定确认邮件队列到交换机
     */
    @Bean
    public Binding confirmationBinding(Queue confirmationQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(confirmationQueue).to(emailExchange).with(CONFIRMATION_ROUTING_KEY);
    }

    /**
     * 消息转换器 (Message Converter)
     * 使用 JSON 格式来序列化和反序列化消息
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}