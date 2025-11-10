package com.example.user.service;

import com.example.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-10
 * 登录安全验证服务
 * 处理长时间未登录的安全验证逻辑
 */
@Slf4j
@Service
public class LoginSecurityService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private EmailVerificationService emailVerificationService;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 长时间未登录的天数阈值（例如：30天）
    private static final long INACTIVE_DAYS_THRESHOLD = 30;
    
    // 安全验证标记前缀
    private static final String SECURITY_VERIFIED_PREFIX = "login:security:verified:";
    
    // 安全验证标记有效期（15分钟）
    private static final int SECURITY_VERIFIED_EXPIRE_MINUTES = 15;
    
    /**
     * 检查用户是否需要安全验证
     * @param email 用户邮箱
     * @return true-需要验证，false-不需要验证
     */
    public boolean needsSecurityVerification(String email) {
        try {
            // 1. 检查是否已经通过了安全验证（15分钟内有效）
            String verifiedKey = SECURITY_VERIFIED_PREFIX + email;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey))) {
                log.info("用户已通过安全验证（15分钟内）: email={}", email);
                return false;
            }
            
            // 2. 获取用户最后登录时间
            LocalDateTime lastLoginAt = userMapper.getLastLoginTime(email);
            
            // 3. 如果从未登录过，不需要安全验证
            if (lastLoginAt == null) {
                log.info("用户首次登录，不需要安全验证: email={}", email);
                return false;
            }
            
            // 4. 计算距离上次登录的天数
            long daysSinceLastLogin = ChronoUnit.DAYS.between(lastLoginAt, LocalDateTime.now());
            
            log.info("用户距离上次登录天数: email={}, days={}", email, daysSinceLastLogin);
            
            // 5. 如果超过阈值，需要安全验证
            if (daysSinceLastLogin >= INACTIVE_DAYS_THRESHOLD) {
                log.warn("用户长时间未登录，需要安全验证: email={}, days={}", email, daysSinceLastLogin);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("检查安全验证失败: email={}", email, e);
            // 出错时为了安全起见，要求验证
            return true;
        }
    }
    
    /**
     * 发送安全验证码
     * @param email 用户邮箱
     * @return 是否发送成功
     */
    public boolean sendSecurityVerificationCode(String email) {
        try {
            return emailVerificationService.sendVerificationCode(email);
        } catch (Exception e) {
            log.error("发送安全验证码失败: email={}", email, e);
            return false;
        }
    }
    
    /**
     * 验证安全验证码
     * @param email 用户邮箱
     * @param code 验证码
     * @return 是否验证成功
     */
    public boolean verifySecurityCode(String email, String code) {
        try {
            // 1. 验证验证码
            boolean valid = emailVerificationService.verifyCode(email, code);
            
            if (valid) {
                // 2. 设置安全验证通过标记（15分钟有效）
                String verifiedKey = SECURITY_VERIFIED_PREFIX + email;
                redisTemplate.opsForValue().set(
                    verifiedKey, 
                    "verified", 
                    SECURITY_VERIFIED_EXPIRE_MINUTES, 
                    TimeUnit.MINUTES
                );
                
                log.info("安全验证通过: email={}", email);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("验证安全验证码失败: email={}", email, e);
            return false;
        }
    }
    
    /**
     * 更新用户最后登录时间
     * @param email 用户邮箱
     */
    public void updateLastLoginTime(String email) {
        try {
            userMapper.updateLastLoginTime(email);
            log.info("更新最后登录时间成功: email={}", email);
        } catch (Exception e) {
            log.error("更新最后登录时间失败: email={}", email, e);
        }
    }
    
    /**
     * 获取用户距离上次登录的天数
     * @param email 用户邮箱
     * @return 天数，如果从未登录返回 -1
     */
    public long getDaysSinceLastLogin(String email) {
        try {
            LocalDateTime lastLoginAt = userMapper.getLastLoginTime(email);
            if (lastLoginAt == null) {
                return -1;
            }
            return ChronoUnit.DAYS.between(lastLoginAt, LocalDateTime.now());
        } catch (Exception e) {
            log.error("获取最后登录时间失败: email={}", email, e);
            return -1;
        }
    }
}
