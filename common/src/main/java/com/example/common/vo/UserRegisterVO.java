package com.example.common.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户注册响应 VO
 */
@Data
public class UserRegisterVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 注册提示信息
     */
    private String tip;
    
    public UserRegisterVO() {
        this.tip = "注册成功！验证邮件已发送到您的邮箱，请查收并完成验证。";
    }
    
    public UserRegisterVO(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.tip = "注册成功！验证邮件已发送到您的邮箱，请查收并完成验证。";
    }
}
