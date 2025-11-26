package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备信息DTO
 * 用于登录时传递设备信息
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
    
    /**
     * 设备ID（客户端生成的唯一标识）
     */
    private String deviceId;
    
    /**
     * 设备类型：WEB, IOS, ANDROID, PC
     */
    private String deviceType;
    
    /**
     * 设备名称
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
     * 从User-Agent解析设备类型
     */
    public static String parseDeviceType(String userAgent) {
        if (userAgent == null) {
            return "WEB";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("android")) {
            return "ANDROID";
        } else if (ua.contains("iphone") || ua.contains("ipad")) {
            return "IOS";
        } else if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("linux")) {
            if (ua.contains("electron")) {
                return "PC";  // Electron桌面应用
            }
            return "WEB";
        }
        
        return "WEB";
    }
    
    /**
     * 从User-Agent解析设备名称
     */
    public static String parseDeviceName(String userAgent) {
        if (userAgent == null) {
            return "Unknown Device";
        }
        
        String ua = userAgent.toLowerCase();
        
        // 浏览器
        if (ua.contains("chrome")) {
            return "Chrome Browser";
        } else if (ua.contains("firefox")) {
            return "Firefox Browser";
        } else if (ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari Browser";
        } else if (ua.contains("edge")) {
            return "Edge Browser";
        }
        
        // 移动设备
        if (ua.contains("android")) {
            return "Android Device";
        } else if (ua.contains("iphone")) {
            return "iPhone";
        } else if (ua.contains("ipad")) {
            return "iPad";
        }
        
        return "Unknown Device";
    }
}
