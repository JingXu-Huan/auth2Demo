package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户会话实体
 * 对应 auth_db.user_sessions 表
 */
@Data
@Accessors(chain = true)
@TableName("user_sessions")
public class UserSession {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    /** 会话ID */
    private String sessionId;
    
    /** 设备ID */
    private Long deviceId;
    
    /** 登录IP */
    private String loginIp;
    
    /** 登录地点 */
    private String loginLocation;
    
    /** 是否活跃 */
    private Boolean isActive;
    
    /** 最后活跃时间 */
    private LocalDateTime lastActiveAt;
    
    /** 过期时间 */
    private LocalDateTime expiresAt;
    
    /** 登出时间 */
    private LocalDateTime logoutAt;
    
    /** 登出原因: manual, expired, kicked, security */
    private String logoutReason;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 登出原因常量
    public static final String LOGOUT_MANUAL = "manual";
    public static final String LOGOUT_EXPIRED = "expired";
    public static final String LOGOUT_KICKED = "kicked";
    public static final String LOGOUT_SECURITY = "security";
}
