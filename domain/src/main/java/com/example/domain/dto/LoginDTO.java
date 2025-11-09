package com.example.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录DTO
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 */
@Data
public class LoginDTO {
    
    /**
     * 用户名或邮箱
     */
    @NotBlank(message = "用户名/邮箱不能为空")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 验证码（如果需要）
     */
    private String verificationCode;
}
