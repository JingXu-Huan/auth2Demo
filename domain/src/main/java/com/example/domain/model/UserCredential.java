package com.example.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户凭证实体
 * 存储用户的登录凭证信息，支持多种登录方式
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
@TableName("user_credentials")
public class UserCredential {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联的用户ID
     */
    private Long userId;
    
    /**
     * 凭证提供方 (email:邮箱密码登录, gitee:Gitee第三方登录)
     */
    private String provider;
    
    /**
     * 提供方的唯一用户ID
     */
    private String providerUserId;
    
    /**
     * 密码哈希值 (仅provider=email时使用)
     * 敏感信息，不应返回给前端
     */
    @JsonIgnore
    private String passwordHash;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
