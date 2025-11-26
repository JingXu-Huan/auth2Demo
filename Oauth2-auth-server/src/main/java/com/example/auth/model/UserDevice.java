package com.example.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户设备实体
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDevice {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 设备唯一标识
     */
    private String deviceId;
    
    /**
     * 设备类型：WEB, IOS, ANDROID, PC
     */
    private String deviceType;
    
    /**
     * 设备名称（如：Chrome on Windows）
     */
    private String deviceName;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * User-Agent
     */
    private String userAgent;
    
    /**
     * 设备状态：ACTIVE-在线，KICKED-被踢下线，OFFLINE-离线
     */
    private String status;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 设备类型枚举
     */
    public enum DeviceType {
        WEB, IOS, ANDROID, PC
    }
    
    /**
     * 设备状态枚举
     */
    public enum DeviceStatus {
        ACTIVE,   // 在线
        KICKED,   // 被踢下线
        OFFLINE   // 离线
    }
}
