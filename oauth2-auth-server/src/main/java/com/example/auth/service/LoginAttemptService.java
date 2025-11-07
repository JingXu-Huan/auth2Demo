package com.example.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 登录尝试服务
 * 防止暴力破解攻击
 */
@Slf4j
@Service
public class LoginAttemptService {
    
    private static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
    private static final String LOGIN_BLOCK_PREFIX = "login:block:";
    
    // 最大失败次数
    private static final int MAX_ATTEMPTS = 5;
    
    // 锁定时间（分钟）
    private static final int LOCK_TIME_MINUTES = 15;
    
    // 失败记录过期时间（分钟）
    private static final int ATTEMPT_EXPIRE_MINUTES = 60;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 登录成功，清除失败记录
     * 
     * @param key 用户标识（用户名或IP）
     */
    public void loginSucceeded(String key) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + key;
        String blockKey = LOGIN_BLOCK_PREFIX + key;
        
        redisTemplate.delete(attemptKey);
        redisTemplate.delete(blockKey);
        
        log.info("登录成功，清除失败记录: key={}", key);
    }
    
    /**
     * 登录失败，记录失败次数
     * 
     * @param key 用户标识（用户名或IP）
     */
    public void loginFailed(String key) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + key;
        
        // 增加失败次数
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        
        if (attempts == null) {
            attempts = 1L;
        }
        
        // 设置过期时间
        if (attempts == 1) {
            redisTemplate.expire(attemptKey, ATTEMPT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        log.warn("登录失败: key={}, attempts={}/{}", key, attempts, MAX_ATTEMPTS);
        
        // 如果超过最大次数，锁定账户
        if (attempts >= MAX_ATTEMPTS) {
            blockUser(key);
        }
    }
    
    /**
     * 锁定用户
     * 
     * @param key 用户标识
     */
    private void blockUser(String key) {
        String blockKey = LOGIN_BLOCK_PREFIX + key;
        redisTemplate.opsForValue().set(blockKey, "blocked", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
        
        log.warn("用户被锁定: key={}, lockTime={}分钟", key, LOCK_TIME_MINUTES);
    }
    
    /**
     * 检查用户是否被锁定
     * 
     * @param key 用户标识
     * @return true=被锁定，false=未锁定
     */
    public boolean isBlocked(String key) {
        String blockKey = LOGIN_BLOCK_PREFIX + key;
        Boolean hasKey = redisTemplate.hasKey(blockKey);
        return Boolean.TRUE.equals(hasKey);
    }
    
    /**
     * 获取剩余尝试次数
     * 
     * @param key 用户标识
     * @return 剩余次数
     */
    public int getRemainingAttempts(String key) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + key;
        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);
        
        if (attemptsStr == null) {
            return MAX_ATTEMPTS;
        }
        
        int attempts = Integer.parseInt(attemptsStr);
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }
    
    /**
     * 获取锁定剩余时间（秒）
     * 
     * @param key 用户标识
     * @return 剩余时间（秒），未锁定返回0
     */
    public long getBlockRemainingTime(String key) {
        String blockKey = LOGIN_BLOCK_PREFIX + key;
        Long expire = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
        return expire != null && expire > 0 ? expire : 0;
    }
}
