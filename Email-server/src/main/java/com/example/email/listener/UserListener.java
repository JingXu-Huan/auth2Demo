package com.example.email.listener;

import com.example.domain.dto.ConfirmationEmailTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 用户相关邮件监听器
 * 监听用户注册、验证等邮件发送任务
 */
@Slf4j
@Component
public class UserListener {
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    private final JavaMailSender mailSender;
    
    public UserListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * 监听邮箱验证邮件队列
     */
    @RabbitListener(queues = "email.verification.queue")
    public void handleVerificationEmail(ConfirmationEmailTask task) {
        try {
            log.info("收到邮箱验证邮件任务: to={}", task.getTo());
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(task.getTo());
            message.setSubject(task.getSubject());
            message.setText(task.getContent());
            
            mailSender.send(message);
            
            log.info("邮箱验证邮件发送成功: to={}", task.getTo());
            
        } catch (Exception e) {
            log.error("发送邮箱验证邮件失败: to={}", task.getTo(), e);
        }
    }
    
    /**
     * 监听欢迎邮件队列
     */
    @RabbitListener(queues = "email.welcome.queue")
    public void handleWelcomeEmail(ConfirmationEmailTask task) {
        try {
            log.info("收到欢迎邮件任务: to={}", task.getTo());
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(task.getTo());
            message.setSubject(task.getSubject());
            message.setText(task.getContent());
            
            mailSender.send(message);
            
            log.info("欢迎邮件发送成功: to={}", task.getTo());
            
        } catch (Exception e) {
            log.error("发送欢迎邮件失败: to={}", task.getTo(), e);
        }
    }
    
    /**
     * 监听确认邮件队列
     */
    @RabbitListener(queues = "email.confirmation.queue")
    public void handleConfirmationEmail(ConfirmationEmailTask task) {
        try {
            log.info("收到确认邮件任务: to={}", task.getTo());
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(task.getTo());
            message.setSubject(task.getSubject());
            message.setText(task.getContent());
            
            mailSender.send(message);
            
            log.info("确认邮件发送成功: to={}", task.getTo());
            
        } catch (Exception e) {
            log.error("发送确认邮件失败: to={}", task.getTo(), e);
        }
    }
}
