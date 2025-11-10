package com.example.domain.dto;

import lombok.Data;

/**
 * 用户详情 DTO
 * 包含敏感信息（passwordHash），仅用于服务间认证
 * 
 * @author Junjie
 * @version 1.0.0
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
