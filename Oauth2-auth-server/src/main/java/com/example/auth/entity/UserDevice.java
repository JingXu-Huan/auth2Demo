package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户设备实体
 * 对应 auth_db.user_devices 表
 */
@Data
@Accessors(chain = true)
@TableName("user_devices")
public class UserDevice {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    /** 设备唯一标识 */
    private String deviceId;
    
    /** 设备类型: web, ios, android, desktop */
    private String deviceType;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 操作系统 */
    private String os;
    
    /** 浏览器 */
    private String browser;
    
    /** User-Agent */
    private String userAgent;
    
    /** 推送Token */
    private String pushToken;
    
    /** 是否信任设备 */
    private Boolean isTrusted;
    
    /** 首次登录时间 */
    private LocalDateTime firstLoginAt;
    
    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;
    
    /** 最后登录IP */
    private String lastLoginIp;
    
    /** 登录次数 */
    private Integer loginCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    // 设备类型常量
    public static final String TYPE_WEB = "web";
    public static final String TYPE_IOS = "ios";
    public static final String TYPE_ANDROID = "android";
    public static final String TYPE_DESKTOP = "desktop";
}
