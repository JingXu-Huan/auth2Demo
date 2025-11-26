package com.example.auth.context;

import com.example.auth.dto.DeviceInfo;

/**
 * 设备信息上下文持有者
 * 使用ThreadLocal在请求线程中传递设备信息
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
public class DeviceContextHolder {
    
    private static final ThreadLocal<DeviceInfo> DEVICE_CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置当前设备信息
     */
    public static void setDevice(DeviceInfo deviceInfo) {
        DEVICE_CONTEXT.set(deviceInfo);
    }
    
    /**
     * 获取当前设备信息
     */
    public static DeviceInfo getDevice() {
        return DEVICE_CONTEXT.get();
    }
    
    /**
     * 获取设备ID
     */
    public static String getDeviceId() {
        DeviceInfo device = DEVICE_CONTEXT.get();
        return device != null ? device.getDeviceId() : null;
    }
    
    /**
     * 清除设备信息
     */
    public static void clear() {
        DEVICE_CONTEXT.remove();
    }
}
