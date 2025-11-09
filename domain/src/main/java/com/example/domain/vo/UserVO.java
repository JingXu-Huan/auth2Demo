package com.example.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 VO
 * 返回给前端的用户信息，不包含敏感数据
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
public class UserVO {
    
    private Long id;
    
    private String username;
    
    private String displayName;
    
    private String email;
    
    private Boolean emailVerified;
    
    private String avatarUrl;
    
    private LocalDateTime createdAt;
}
