package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * OAuth2令牌实体
 * 对应 auth_db.oauth2_tokens 表
 */
@Data
@Accessors(chain = true)
@TableName("oauth2_tokens")
public class OAuth2Token {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 客户端ID */
    private String clientId;
    
    /** 访问令牌 */
    private String accessToken;
    
    /** 刷新令牌 */
    private String refreshToken;
    
    /** 令牌类型 */
    private String tokenType;
    
    /** 权限范围 */
    private String scope;
    
    /** 过期时间 */
    private LocalDateTime expiresAt;
    
    /** 设备ID */
    private String deviceId;
    
    /** 设备类型: web, mobile, desktop */
    private String deviceType;
    
    /** User-Agent */
    private String userAgent;
    
    /** IP地址 */
    private String ipAddress;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 令牌类型常量
    public static final String TYPE_BEARER = "Bearer";
}
