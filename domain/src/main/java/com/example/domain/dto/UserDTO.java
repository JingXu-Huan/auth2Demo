package com.example.domain.dto;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 用户 DTO
 * 用于服务间传输用户基本信息
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
public class UserDTO {
    
    private Long id;
    
    private String username;
    
    private String displayName;
    
    private String email;
    
    private String password;
    
    private Boolean emailVerified;
    
    private String avatarUrl;
    
    private OffsetDateTime createdAt;
    
    private OffsetDateTime updatedAt;
}
