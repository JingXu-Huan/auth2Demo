package com.example.common.dto;

import lombok.Data;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户详情 DTO（共享模块）
 * ⚠️ 包含敏感信息（passwordHash），仅用于服务间认证
 */
@Data
public class UserDetailsDTO {
    
    private Long userId;
    
    private String username;
    
    private String displayName;
    
    private String email;
    
    private Boolean emailVerified;
    
    private String avatarUrl;
    
    /**
     * 密码哈希 - 仅用于内部认证，不应返回给前端
     */
    private String passwordHash;
    
    private String provider;
}
