package com.example.auth.service;

import com.example.auth.dto.DeviceInfo;
import com.example.auth.mapper.UserDeviceMapper;
import com.example.auth.model.UserDevice;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 设备管理服务
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Slf4j
@Service
public class DeviceService {
    
    @Autowired
    private UserDeviceMapper deviceMapper;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    /**
     * 登录时更新设备信息
     * 
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void upsertDevice(Long userId, DeviceInfo deviceInfo) {
        log.info("更新设备信息: userId={}, deviceId={}, deviceType={}", 
            userId, deviceInfo.getDeviceId(), deviceInfo.getDeviceType());
        
        UserDevice device = UserDevice.builder()
            .userId(userId)
            .deviceId(deviceInfo.getDeviceId())
            .deviceType(deviceInfo.getDeviceType())
            .deviceName(deviceInfo.getDeviceName())
            .ipAddress(deviceInfo.getIpAddress())
            .userAgent(deviceInfo.getUserAgent())
            .status(UserDevice.DeviceStatus.ACTIVE.name())
            .lastActiveAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
        
        // Upsert到数据库
        deviceMapper.insertOrUpdate(device);
        
        log.info("设备信息更新成功: userId={}, deviceId={}", userId, deviceInfo.getDeviceId());
    }
    
    /**
     * 关联RefreshToken与设备
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @param refreshToken RefreshToken
     */
    public void associateRefreshToken(Long userId, String deviceId, String refreshToken) {
        // Key: auth:device:{userId}:{deviceId} -> refreshToken
        String deviceKey = "auth:device:" + userId + ":" + deviceId;
        redisTemplate.opsForValue().set(deviceKey, refreshToken, 7, TimeUnit.DAYS);
        
        log.info("RefreshToken关联设备: userId={}, deviceId={}", userId, deviceId);
    }
    
    /**
     * 获取用户所有设备
     * 
     * @param userId 用户ID
     * @return 设备列表
     */
    public List<UserDevice> getUserDevices(Long userId) {
        return deviceMapper.findByUserId(userId);
    }
    
    /**
     * 踢下线指定设备
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void kickDevice(Long userId, String deviceId) {
        log.warn("踢下线设备: userId={}, deviceId={}", userId, deviceId);
        
        // 1. 数据库标记为被踢
        deviceMapper.updateStatus(userId, deviceId, UserDevice.DeviceStatus.KICKED.name());
        
        // 2. Redis写入踢人标记（网关鉴权时会检查）
        // Key: auth:kick:{userId}:{deviceId}, Value: 1, TTL: 15分钟（Access Token有效期）
        String kickKey = "auth:kick:" + userId + ":" + deviceId;
        redisTemplate.opsForValue().set(kickKey, "1", 15, TimeUnit.MINUTES);
        
        // 3. 删除该设备的Refresh Token
        String deviceKey = "auth:device:" + userId + ":" + deviceId;
        String refreshToken = redisTemplate.opsForValue().get(deviceKey);
        if (refreshToken != null) {
            // 删除设备映射
            redisTemplate.delete(deviceKey);
            // 删除Refresh Token
            redisTemplate.delete("auth:refresh:" + refreshToken);
            log.info("已删除设备的RefreshToken: userId={}, deviceId={}", userId, deviceId);
        }
        
        // 4. 发送MQ事件（通知IM网关断开连接）
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("deviceId", deviceId);
            event.put("eventType", "KICK_OUT");
            event.put("timestamp", System.currentTimeMillis());
            
            rocketMQTemplate.convertAndSend("RISK_EVENT:KICK_OUT", event);
            log.info("已发送踢人MQ事件: userId={}, deviceId={}", userId, deviceId);
        } catch (Exception e) {
            log.error("发送踢人MQ事件失败: userId={}, deviceId={}", userId, deviceId, e);
            // 不影响主流程，继续执行
        }
        
        log.warn("设备踢下线成功: userId={}, deviceId={}", userId, deviceId);
    }
    
    /**
     * 踢下线用户的所有设备
     * 
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void kickAllDevices(Long userId) {
        log.warn("踢下线用户所有设备: userId={}", userId);
        
        // 1. 查询所有活跃设备
        List<UserDevice> devices = deviceMapper.findByUserId(userId);
        
        // 2. 批量踢下线
        for (UserDevice device : devices) {
            if (UserDevice.DeviceStatus.ACTIVE.name().equals(device.getStatus())) {
                kickDevice(userId, device.getDeviceId());
            }
        }
        
        log.warn("用户所有设备已踢下线: userId={}, count={}", userId, devices.size());
    }
    
    /**
     * 更新设备最后活跃时间
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    public void updateLastActiveTime(Long userId, String deviceId) {
        deviceMapper.updateLastActiveTime(userId, deviceId, LocalDateTime.now());
    }
    
    /**
     * 检查设备是否被踢下线
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return true-被踢下线，false-正常
     */
    public boolean isDeviceKicked(Long userId, String deviceId) {
        String kickKey = "auth:kick:" + userId + ":" + deviceId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(kickKey));
    }
    
    /**
     * 统计用户活跃设备数
     * 
     * @param userId 用户ID
     * @return 活跃设备数
     */
    public int countActiveDevices(Long userId) {
        return deviceMapper.countActiveDevices(userId);
    }
    
    /**
     * 从请求中解析设备信息
     * 
     * @param deviceId 设备ID
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @return 设备信息
     */
    public DeviceInfo parseDeviceInfo(String deviceId, String ipAddress, String userAgent) {
        return DeviceInfo.builder()
            .deviceId(deviceId != null ? deviceId : generateDeviceId(userAgent, ipAddress))
            .deviceType(DeviceInfo.parseDeviceType(userAgent))
            .deviceName(DeviceInfo.parseDeviceName(userAgent))
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();
    }
    
    /**
     * 生成设备ID（如果客户端未提供）
     */
    private String generateDeviceId(String userAgent, String ipAddress) {
        String raw = userAgent + "|" + ipAddress + "|" + System.currentTimeMillis();
        return "device-" + Math.abs(raw.hashCode());
    }
}
