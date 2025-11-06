package com.example.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户视图对象 VO
 * 用于前端展示，不包含任何敏感信息
 */
@Data
public class UserVO implements Serializable {
    
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
     * 显示名称
     */
    private String displayName;
    
    /**
     * 邮箱（可选择性展示）
     */
    private String email;
    
    /**
     * 邮箱是否已验证
     */
    private Boolean emailVerified;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 用户状态描述
     */
    private String statusText;
    
    /**
     * 获取状态文本
     */
    public String getStatusText() {
        if (Boolean.TRUE.equals(emailVerified)) {
            return "已验证";
        } else {
            return "未验证";
        }
    }
}
