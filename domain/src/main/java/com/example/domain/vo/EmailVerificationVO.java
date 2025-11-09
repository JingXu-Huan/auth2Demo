package com.example.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮箱验证 VO
 * 返回给前端的邮箱验证信息
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
public class EmailVerificationVO {
    
    /**
     * 邮箱地址
     */
    private String email;
    
    /**
     * 验证码类型
     */
    private String codeType;
    
    /**
     * 是否已发送
     */
    private Boolean sent;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 提示消息
     */
    private String message;
}
