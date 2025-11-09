package com.example.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册响应 VO
 * 返回给前端的注册结果
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterVO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 注册状态消息
     */
    private String message;
    
    /**
     * 是否需要邮箱验证
     */
    private Boolean requiresEmailVerification;
}
