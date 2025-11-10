package com.example.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮箱验证码实体
 * 存储邮箱验证码信息，用于注册和安全验证
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
@TableName("email_verification_codes")
public class EmailVerificationCode {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 接收验证码的邮箱
     */
    private String email;
    
    /**
     * 验证码
     */
    private String code;
    
    /**
     * 验证码类型 (register:注册, security:安全验证, reset_password:重置密码)
     */
    private String codeType;
    
    /**
     * 验证码过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 是否已验证 (0:未验证, 1:已验证)
     */
    private Boolean verified;
    
    /**
     * 验证时间
     */
    private LocalDateTime verifiedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
