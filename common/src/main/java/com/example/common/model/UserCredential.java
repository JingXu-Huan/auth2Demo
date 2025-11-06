package com.example.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户凭证表实体（共享模块）
 */
@Data
@TableName("user_credentials")
public class UserCredential {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    /**
     * 凭证提供方: email, gitee
     */
    private String provider;
    
    /**
     * 提供方的唯一ID
     */
    private String providerUserId;
    
    /**
     * 密码Hash (仅 provider='email' 时使用)
     * 敏感信息，不应返回给前端
     */
    @JsonIgnore
    private String passwordHash;
    
    private LocalDateTime createdAt;
}
