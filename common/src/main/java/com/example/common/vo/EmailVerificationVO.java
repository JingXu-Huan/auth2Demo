package com.example.common.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 邮箱验证响应 VO
 */
@Data
public class EmailVerificationVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 邮箱地址
     */
    private String email;
    
    /**
     * 验证状态
     */
    private Boolean verified;
    
    /**
     * 提示信息
     */
    private String message;
    
    public EmailVerificationVO() {
    }
    
    public EmailVerificationVO(String email, Boolean verified, String message) {
        this.email = email;
        this.verified = verified;
        this.message = message;
    }
    
    /**
     * 创建验证成功响应
     */
    public static EmailVerificationVO success(String email) {
        return new EmailVerificationVO(email, true, "邮箱验证成功！");
    }
    
    /**
     * 创建验证失败响应
     */
    public static EmailVerificationVO fail(String email, String message) {
        return new EmailVerificationVO(email, false, message);
    }
}
