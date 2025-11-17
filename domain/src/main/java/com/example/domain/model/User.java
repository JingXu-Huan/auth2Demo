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
    
    private String phone;
    
    private String nickname;
    
    private Boolean emailVerified;
    
    private Boolean phoneVerified;
    
    private String avatarUrl;
    
    private String signature;
    
    private String gender;
    
    private java.sql.Date birthday;
    
    private String location;
    
    private String status;
    
    private String lastLoginIp;
    
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
    
    /**
     * 账户是否启用（默认启用）
     */
    private Boolean enabled;
    
    /**
     * 账户是否过期（默认未过期）
     */
    @JsonIgnore
    private Boolean accountNonExpired;
    
    /**
     * 账户是否锁定 - 管理员手动锁定（默认未锁定）
     */
    @JsonIgnore
    private Boolean accountNonLocked;
    
    /**
     * 密码是否过期（默认未过期）
     */
    @JsonIgnore
    private Boolean credentialsNonExpired;
    
    /**
     * 账户锁定原因
     */
    @JsonIgnore
    private String lockReason;
    
    /**
     * 账户锁定时间
     */
    @JsonIgnore
    private LocalDateTime lockedAt;
}
