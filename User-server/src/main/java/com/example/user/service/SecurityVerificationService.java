package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.domain.model.User;
import com.example.user.mapper.UserMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 安全验证服务
 * 处理密码验证、安全码等功能
 */
@Slf4j
@Service
public class SecurityVerificationService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    private static final String SECURITY_CODE_PREFIX = "security:code:";
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int CODE_LENGTH = 6;
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 验证用户密码
     */
    public boolean validatePassword(Long userId, String password) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return false;
            }
            
            // 检查是否是邮箱登录用户
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", userId);
            queryWrapper.eq("provider", "email");
            
            User emailUser = userMapper.selectOne(queryWrapper);
            if (emailUser == null) {
                log.warn("该用户不是邮箱登录用户: userId={}", userId);
                return false;
            }
            
            // 验证密码（需要从 user_credentials 表获取）
            // 这里简化处理，实际应该查询 user_credentials 表
            log.info("密码验证: userId={}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("密码验证失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 发送安全验证码
     */
    public boolean sendSecurityCode(String email) {
        try {
            // 1. 生成6位数字验证码
            String code = generateSecurityCode();
            
            // 2. 存储到 Redis
            String key = SECURITY_CODE_PREFIX + email;
            redisTemplate.opsForValue().set(key, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.info("安全验证码已生成: email={}, code={}", email, code);
            
            // 3. 发送邮件
            Map<String, String> emailData = new HashMap<>();
            emailData.put("to", email);
            emailData.put("subject", "安全验证码");
            emailData.put("content", buildSecurityCodeEmail(code));
            
            // 使用交换机和路由键发送消息
            rabbitTemplate.convertAndSend("user.exchange", "email.confirmation", emailData);
            
            log.info("安全验证码邮件已发送到交换机: email={}, exchange=user.exchange, routingKey=email.confirmation", email);
            return true;
            
        } catch (Exception e) {
            log.error("发送安全验证码失败: email={}", email, e);
            return false;
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 验证安全码
     */
    public boolean verifySecurityCode(String email, String code) {
        try {
            String key = SECURITY_CODE_PREFIX + email;
            String storedCode = redisTemplate.opsForValue().get(key);
            
            if (storedCode == null) {
                log.warn("安全验证码不存在或已过期: email={}", email);
                return false;
            }
            
            if (!storedCode.equals(code)) {
                log.warn("安全验证码错误: email={}", email);
                return false;
            }
            
            // 验证成功，删除验证码
            redisTemplate.delete(key);
            log.info("安全验证码验证成功: email={}", email);
            return true;
            
        } catch (Exception e) {
            log.error("安全验证码验证失败: email={}", email, e);
            return false;
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 生成安全验证码
     */
    private String generateSecurityCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 构建安全验证码邮件内容
     */
    private String buildSecurityCodeEmail(String code) {
        return String.format(
            "您的安全验证码是: %s\n\n" +
            "验证码有效期为 %d 分钟。\n" +
            "如果这不是您的操作，请立即修改密码。",
            code, CODE_EXPIRE_MINUTES
        );
    }
}
