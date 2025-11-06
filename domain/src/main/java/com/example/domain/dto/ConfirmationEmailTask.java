package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 确认邮件任务 DTO
 * 用于 RabbitMQ 消息队列传输
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationEmailTask implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱地址
     */
    private String email;
    
    /**
     * 收件人邮箱（用于发送邮件）
     */
    private String to;
    
    /**
     * 邮件主题
     */
    private String subject;
    
    /**
     * 邮件内容
     */
    private String content;
    
    /**
     * 确认令牌
     */
    private String confirmationToken;
    
    /**
     * 确认链接
     */
    private String confirmationUrl;
}
