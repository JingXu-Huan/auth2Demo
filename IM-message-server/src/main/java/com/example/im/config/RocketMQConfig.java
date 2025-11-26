package com.example.im.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * RocketMQ 配置
 * 确保事务消息正常工作
 */
@Configuration
public class RocketMQConfig {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @PostConstruct
    public void init() {
        // 设置事务消息的超时时间
        rocketMQTemplate.getProducer().setSendMsgTimeout(10000);
    }
}
