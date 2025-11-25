package com.example.email.listener;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 邮件监听器
 * 监听邮件队列并发送邮件
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "user_email_topic",
    consumerGroup = "email_consumer_group",
    selectorExpression = "email_verification"  // 只消费 email_verification 标签的消息
)
public class EmailListener implements RocketMQListener<String> {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 监听邮件发送队列（RocketMQ 消费者）
     */
    @Override
    public void onMessage(String message) {
        try {
            log.info("收到 RocketMQ 邮件发送请求: {}", message);
            
            // 解析 JSON 消息
            @SuppressWarnings("unchecked")
            Map<String, Object> emailData = JSON.parseObject(message, Map.class);
            
            String to = (String) emailData.get("to");
            String subject = (String) emailData.get("subject");
            String content = (String) emailData.get("content");
            
            log.info("解析邮件数据: to={}, subject={}", to, subject);
            
            sendEmail(to, subject, content);
            
            log.info("邮件发送成功: to={}", to);
            
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            // 抛出异常触发 RocketMQ 重试机制
            throw new RuntimeException("邮件发送失败，请求重试", e);
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 发送邮件的通用方法
     */
    private void sendEmail(String to, String subject, String content) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true 表示支持 HTML
        
        mailSender.send(message);
    }
}
