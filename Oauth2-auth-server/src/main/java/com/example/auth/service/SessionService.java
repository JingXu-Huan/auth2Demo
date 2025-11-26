package com.example.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.domain.model.UserSession;
import com.example.auth.mapper.UserSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 会话管理服务
 * 处理用户会话的创建、验证、更新和清理
 */
@Slf4j
@Service
public class SessionService {
    
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final long SESSION_TIMEOUT_HOURS = 24;
    private static final int MAX_SESSIONS_PER_USER = 5;
    
    @Autowired
    private UserSessionMapper sessionMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 创建新会话
     */
    @Transactional
    public UserSession createSession(Long userId, HttpServletRequest request) {
        try {
            // 生成会话ID
            String sessionId = UUID.randomUUID().toString();
            
            // 获取设备信息
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String deviceId = request.getHeader("X-Device-Id");
            String deviceType = parseDeviceType(userAgent);
            
            // 创建会话
            UserSession session = new UserSession();
            session.setId(sessionId);
            session.setUserId(userId);
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);
            session.setDeviceId(deviceId);
            session.setDeviceType(deviceType);
            session.setCreatedAt(LocalDateTime.now());
            session.setLastAccessedAt(LocalDateTime.now());
            session.setExpiresAt(LocalDateTime.now().plusHours(SESSION_TIMEOUT_HOURS));
            
            // 设置会话数据
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("loginTime", System.currentTimeMillis());
            sessionData.put("browser", parseBrowser(userAgent));
            sessionData.put("os", parseOS(userAgent));
            session.setSessionData(sessionData);
            
            // 保存到数据库
            sessionMapper.insert(session);
            
            // 缓存到Redis
            cacheSession(session);
            
            // 检查并清理多余的会话
            cleanupExcessSessions(userId);
            
            log.info("创建会话成功: sessionId={}, userId={}, ip={}", 
                sessionId, userId, ipAddress);
            
            return session;
        } catch (Exception e) {
            log.error("创建会话失败: userId={}", userId, e);
            throw new RuntimeException("创建会话失败", e);
        }
    }
    
    /**
     * 验证会话
     */
    public boolean validateSession(String sessionId) {
        try {
            // 先从Redis查询
            UserSession session = getCachedSession(sessionId);
            
            if (session == null) {
                // Redis中没有，从数据库查询
                session = sessionMapper.selectById(sessionId);
                if (session != null && session.isActive()) {
                    // 重新缓存
                    cacheSession(session);
                }
            }
            
            if (session == null || !session.isActive()) {
                return false;
            }
            
            // 更新最后访问时间
            updateLastAccessTime(sessionId);
            
            return true;
        } catch (Exception e) {
            log.error("验证会话失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 获取会话信息
     */
    public UserSession getSession(String sessionId) {
        try {
            // 先从Redis查询
            UserSession session = getCachedSession(sessionId);
            
            if (session == null) {
                // 从数据库查询
                session = sessionMapper.selectById(sessionId);
                if (session != null && session.isActive()) {
                    cacheSession(session);
                }
            }
            
            return session;
        } catch (Exception e) {
            log.error("获取会话失败: sessionId={}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 销毁会话（登出）
     */
    @Transactional
    public void destroySession(String sessionId) {
        try {
            // 删除数据库记录
            sessionMapper.deleteById(sessionId);
            
            // 删除Redis缓存
            redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
            
            log.info("销毁会话成功: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("销毁会话失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 销毁用户的所有会话
     */
    @Transactional
    public void destroyUserSessions(Long userId) {
        try {
            // 查询用户的所有会话
            List<UserSession> sessions = sessionMapper.selectList(
                new QueryWrapper<UserSession>().eq("user_id", userId)
            );
            
            // 删除数据库记录
            sessionMapper.delete(
                new QueryWrapper<UserSession>().eq("user_id", userId)
            );
            
            // 删除Redis缓存
            for (UserSession session : sessions) {
                redisTemplate.delete(SESSION_KEY_PREFIX + session.getId());
            }
            
            log.info("销毁用户所有会话成功: userId={}, count={}", 
                userId, sessions.size());
        } catch (Exception e) {
            log.error("销毁用户会话失败: userId={}", userId, e);
        }
    }
    
    /**
     * 获取用户的活跃会话列表
     */
    public List<UserSession> getUserActiveSessions(Long userId) {
        try {
            return sessionMapper.selectList(
                new QueryWrapper<UserSession>()
                    .eq("user_id", userId)
                    .gt("expires_at", LocalDateTime.now())
                    .orderByDesc("last_accessed_at")
            );
        } catch (Exception e) {
            log.error("获取用户会话列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 更新最后访问时间
     */
    private void updateLastAccessTime(String sessionId) {
        try {
            sessionMapper.updateLastAccessTime(sessionId);
            
            // 更新Redis缓存
            UserSession session = getCachedSession(sessionId);
            if (session != null) {
                session.setLastAccessedAt(LocalDateTime.now());
                cacheSession(session);
            }
        } catch (Exception e) {
            log.error("更新会话访问时间失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 清理过期会话
     */
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            // 查询过期会话
            List<UserSession> expiredSessions = sessionMapper.selectList(
                new QueryWrapper<UserSession>()
                    .lt("expires_at", LocalDateTime.now())
            );
            
            if (expiredSessions.isEmpty()) {
                return;
            }
            
            // 删除数据库记录
            sessionMapper.delete(
                new QueryWrapper<UserSession>()
                    .lt("expires_at", LocalDateTime.now())
            );
            
            // 删除Redis缓存
            for (UserSession session : expiredSessions) {
                redisTemplate.delete(SESSION_KEY_PREFIX + session.getId());
            }
            
            log.info("清理过期会话成功: count={}", expiredSessions.size());
        } catch (Exception e) {
            log.error("清理过期会话失败", e);
        }
    }
    
    /**
     * 清理多余的会话（保持每个用户最多N个会话）
     */
    private void cleanupExcessSessions(Long userId) {
        try {
            List<UserSession> sessions = getUserActiveSessions(userId);
            
            if (sessions.size() > MAX_SESSIONS_PER_USER) {
                // 按最后访问时间排序，删除最旧的会话
                sessions.sort((a, b) -> b.getLastAccessedAt().compareTo(a.getLastAccessedAt()));
                
                for (int i = MAX_SESSIONS_PER_USER; i < sessions.size(); i++) {
                    destroySession(sessions.get(i).getId());
                }
                
                log.info("清理多余会话: userId={}, removed={}", 
                    userId, sessions.size() - MAX_SESSIONS_PER_USER);
            }
        } catch (Exception e) {
            log.error("清理多余会话失败: userId={}", userId, e);
        }
    }
    
    /**
     * 缓存会话到Redis
     */
    private void cacheSession(UserSession session) {
        String key = SESSION_KEY_PREFIX + session.getId();
        redisTemplate.opsForValue().set(key, session, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
    }
    
    /**
     * 从Redis获取会话
     */
    private UserSession getCachedSession(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        return (UserSession) redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 解析设备类型
     */
    private String parseDeviceType(String userAgent) {
        if (userAgent == null) {
            return "UNKNOWN";
        }
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "MOBILE";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "TABLET";
        } else {
            return "DESKTOP";
        }
    }
    
    /**
     * 解析浏览器
     */
    private String parseBrowser(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        if (userAgent.contains("Chrome")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari")) {
            return "Safari";
        } else if (userAgent.contains("Edge")) {
            return "Edge";
        } else {
            return "Other";
        }
    }
    
    /**
     * 解析操作系统
     */
    private String parseOS(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac")) {
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iOS") || userAgent.contains("iPhone")) {
            return "iOS";
        } else {
            return "Other";
        }
    }
}
