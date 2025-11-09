package com.example.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户主表实体
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
@TableName("users")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String displayName;
    
    private String email;
    
    private Boolean emailVerified;
    
    private String avatarUrl;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    /**
     * 邮箱验证令牌 - 敏感信息，不应返回给前端
     */
    @JsonIgnore
    private String confirmationToken;

    /**
     * 令牌过期时间 - 敏感信息，不应返回给前端
     */
    @JsonIgnore
    private LocalDateTime tokenExpiry;
    
    /**
     * 最后登录时间 - 敏感信息，不应返回给前端
     */
    @JsonIgnore
    private LocalDateTime lastLoginAt;
}
