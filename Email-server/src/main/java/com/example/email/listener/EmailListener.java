package com.example.email.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.util.Map;

/**
 * 邮件监听器
 * 监听邮件队列并发送邮件
 */
@Slf4j
@Component
public class EmailListener {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    /**
     * 监听邮件发送队列（安全验证码等）
     */
    @RabbitListener(queues = "email.send.queue")
    public void handleEmailSend(Map<String, Object> emailData) {
        try {
            String to = (String) emailData.get("to");
            String subject = (String) emailData.get("subject");
            String content = (String) emailData.get("content");
            
            log.info("收到邮件发送请求: to={}, subject={}", to, subject);
            
            sendEmail(to, subject, content);
            
            log.info("邮件发送成功: to={}", to);
            
        } catch (Exception e) {
            log.error("邮件发送失败", e);
        }
    }
    
    /**
     * 监听用户注册确认邮件队列
     */
    @RabbitListener(queues = "email.confirmation.queue")
    public void handleRegistrationConfirm(Map<String, Object> emailData) {
        try {
            String to = (String) emailData.get("to");
            String subject = (String) emailData.get("subject");
            String content = (String) emailData.get("content");
            
            log.info("收到注册确认邮件请求: to={}, subject={}", to, subject);
            
            sendEmail(to, subject, content);
            
            log.info("注册确认邮件发送成功: to={}", to);
            
        } catch (Exception e) {
            log.error("注册确认邮件发送失败", e);
        }
    }
    
    /**
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
