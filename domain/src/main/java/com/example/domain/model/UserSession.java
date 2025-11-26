package com.example.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户会话实体
 * 对应 user_sessions 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "user_sessions", autoResultMap = true)
public class UserSession implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID（主键）
     */
    @TableId(type = IdType.INPUT)
    private String id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * User-Agent
     */
    private String userAgent;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 设备类型
     */
    private String deviceType;
    
    /**
     * 会话数据（JSONB）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> sessionData;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedAt;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 是否活跃
     */
    @TableField(exist = false)
    private Boolean active;
    
    /**
     * 判断会话是否过期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * 判断会话是否活跃
     */
    public boolean isActive() {
        return !isExpired();
    }
}
