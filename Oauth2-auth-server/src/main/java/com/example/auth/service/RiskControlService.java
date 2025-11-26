package com.example.auth.service;

import com.example.auth.mapper.UserPunishmentMapper;
import com.example.auth.model.UserPunishment;
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
 * 风控服务
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Slf4j
@Service
public class RiskControlService {
    
    @Autowired
    private UserPunishmentMapper punishmentMapper;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private DeviceService deviceService;
    
    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;
    
    /**
     * 封禁用户
     * 
     * @param userId 被封禁的用户ID
     * @param durationSeconds 封禁时长（秒），null表示永久
     * @param reason 封禁原因
     * @param operatorId 操作人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long userId, Integer durationSeconds, String reason, Long operatorId) {
        log.warn("封禁用户: userId={}, duration={}, reason={}, operator={}", 
            userId, durationSeconds, reason, operatorId);
        
        // 1. 插入惩罚记录
        LocalDateTime expiresAt = durationSeconds != null ? 
            LocalDateTime.now().plusSeconds(durationSeconds) : null;
        
        UserPunishment punishment = UserPunishment.builder()
            .userId(userId)
            .type(UserPunishment.PunishmentType.BAN.name())
            .duration(durationSeconds)
            .reason(reason)
            .operatorId(operatorId)
            .createdAt(LocalDateTime.now())
            .expiresAt(expiresAt)
            .revoked(false)
            .build();
        
        punishmentMapper.insert(punishment);
        
        // 2. Redis封禁标记（网关会检查）
        String banKey = "risk:ban:user:" + userId;
        if (durationSeconds != null) {
            // 有期限封禁
            redisTemplate.opsForValue().set(banKey, "1", durationSeconds, TimeUnit.SECONDS);
        } else {
            // 永久封禁
            redisTemplate.opsForValue().set(banKey, "1");
        }
        
        // 3. 清除所有会话（踢下线所有设备）
        deviceService.kickAllDevices(userId);
        
        // 4. 广播MQ事件（通知IM、Gateway等服务）
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("duration", durationSeconds);
            event.put("reason", reason);
            event.put("operatorId", operatorId);
            event.put("eventType", "BAN");
            event.put("timestamp", System.currentTimeMillis());
            
            rocketMQTemplate.convertAndSend("RISK_EVENT:BAN", event);
            log.info("已发送封禁MQ事件: userId={}", userId);
        } catch (Exception e) {
            log.error("发送封禁MQ事件失败: userId={}", userId, e);
            // 不影响主流程
        }
        
        log.warn("用户封禁成功: userId={}, punishmentId={}", userId, punishment.getId());
    }
    
    /**
     * 解封用户
     * 
     * @param userId 用户ID
     * @param revokeReason 解封原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(Long userId, String revokeReason) {
        log.info("解封用户: userId={}, reason={}", userId, revokeReason);
        
        // 1. 撤销数据库中的封禁记录
        int affected = punishmentMapper.revoke(
            userId, 
            UserPunishment.PunishmentType.BAN.name(), 
            LocalDateTime.now(), 
            revokeReason
        );
        
        if (affected == 0) {
            log.warn("未找到该用户的封禁记录: userId={}", userId);
            return;
        }
        
        // 2. 删除Redis封禁标记
        String banKey = "risk:ban:user:" + userId;
        redisTemplate.delete(banKey);
        
        // 3. 发送MQ事件
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("eventType", "UNBAN");
            event.put("revokeReason", revokeReason);
            event.put("timestamp", System.currentTimeMillis());
            
            rocketMQTemplate.convertAndSend("RISK_EVENT:UNBAN", event);
            log.info("已发送解封MQ事件: userId={}", userId);
        } catch (Exception e) {
            log.error("发送解封MQ事件失败: userId={}", userId, e);
        }
        
        log.info("用户解封成功: userId={}", userId);
    }
    
    /**
     * 禁言用户
     * 
     * @param userId 用户ID
     * @param durationSeconds 禁言时长（秒）
     * @param reason 禁言原因
     * @param operatorId 操作人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void muteUser(Long userId, Integer durationSeconds, String reason, Long operatorId) {
        log.warn("禁言用户: userId={}, duration={}, reason={}", userId, durationSeconds, reason);
        
        LocalDateTime expiresAt = durationSeconds != null ? 
            LocalDateTime.now().plusSeconds(durationSeconds) : null;
        
        UserPunishment punishment = UserPunishment.builder()
            .userId(userId)
            .type(UserPunishment.PunishmentType.MUTE.name())
            .duration(durationSeconds)
            .reason(reason)
            .operatorId(operatorId)
            .createdAt(LocalDateTime.now())
            .expiresAt(expiresAt)
            .revoked(false)
            .build();
        
        punishmentMapper.insert(punishment);
        
        // Redis禁言标记
        String muteKey = "risk:mute:user:" + userId;
        if (durationSeconds != null) {
            redisTemplate.opsForValue().set(muteKey, "1", durationSeconds, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(muteKey, "1");
        }
        
        // 发送MQ事件
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("duration", durationSeconds);
            event.put("reason", reason);
            event.put("eventType", "MUTE");
            event.put("timestamp", System.currentTimeMillis());
            
            rocketMQTemplate.convertAndSend("RISK_EVENT:MUTE", event);
        } catch (Exception e) {
            log.error("发送禁言MQ事件失败: userId={}", userId, e);
        }
        
        log.warn("用户禁言成功: userId={}", userId);
    }
    
    /**
     * 取消禁言
     * 
     * @param userId 用户ID
     * @param revokeReason 取消原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void unmuteUser(Long userId, String revokeReason) {
        log.info("取消禁言: userId={}", userId);
        
        punishmentMapper.revoke(
            userId, 
            UserPunishment.PunishmentType.MUTE.name(), 
            LocalDateTime.now(), 
            revokeReason
        );
        
        String muteKey = "risk:mute:user:" + userId;
        redisTemplate.delete(muteKey);
        
        log.info("取消禁言成功: userId={}", userId);
    }
    
    /**
     * 检查用户是否被封禁（网关调用）
     * 
     * @param userId 用户ID
     * @return true-被封禁，false-正常
     */
    public boolean isUserBanned(Long userId) {
        String banKey = "risk:ban:user:" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(banKey));
    }
    
    /**
     * 检查用户是否被禁言
     * 
     * @param userId 用户ID
     * @return true-被禁言，false-正常
     */
    public boolean isUserMuted(Long userId) {
        String muteKey = "risk:mute:user:" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(muteKey));
    }
    
    /**
     * 获取用户的惩罚记录
     * 
     * @param userId 用户ID
     * @return 惩罚记录列表
     */
    public List<UserPunishment> getUserPunishments(Long userId) {
        return punishmentMapper.findByUserId(userId);
    }
    
    /**
     * 获取用户当前的有效封禁
     * 
     * @param userId 用户ID
     * @return 封禁记录，如果未被封禁则返回null
     */
    public UserPunishment getActiveBan(Long userId) {
        return punishmentMapper.findActivePunishment(
            userId, 
            UserPunishment.PunishmentType.BAN.name()
        );
    }
}
