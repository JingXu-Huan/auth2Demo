package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.user.mapper.UserMapper;
import com.example.domain.model.User;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证服务
 * 处理邮箱验证码的发送和验证
 */
@Slf4j
@Service
public class EmailVerificationService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private UserMapper userMapper;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    private static final String VERIFICATION_CODE_PREFIX = "email:verification:";
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int CODE_LENGTH = 6;
    
    /**
     * 发送验证码到指定邮箱
     */
    public boolean sendVerificationCode(String email) {
        try {
            // 1. 生成6位数字验证码
            String code = generateVerificationCode();
            
            // 2. 存储到 Redis，5分钟过期
            String key = VERIFICATION_CODE_PREFIX + email;
            redisTemplate.opsForValue().set(key, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.info("验证码已生成并存储到 Redis: email={}, code={}", email, code);
            
            // 3. 发送邮件（通过 RabbitMQ 异步发送）
            Map<String, String> emailData = new HashMap<>();
            emailData.put("to", email);
            emailData.put("subject", "邮箱验证码");
            emailData.put("content", buildEmailContent(code));
            
            // 使用交换机和路由键发送消息
            rabbitTemplate.convertAndSend("user.exchange", "email.verification", emailData);
            
            log.info("验证码邮件已发送到交换机: email={}, exchange=user.exchange, routingKey=email.verification", email);
            return true;
            
        } catch (Exception e) {
            log.error("发送验证码失败: email={}", email, e);
            return false;
        }
    }
    
    /**
     * 验证邮箱验证码
     */
    public boolean verifyCode(String email, String code) {
        try {
            String key = VERIFICATION_CODE_PREFIX + email;
            String storedCode = redisTemplate.opsForValue().get(key);
            
            if (storedCode == null) {
                log.warn("验证码不存在或已过期: email={}", email);
                return false;
            }
            
            if (!storedCode.equals(code)) {
                log.warn("验证码错误: email={}, input={}, expected={}", email, code, storedCode);
                return false;
            }
            
            // 验证成功，删除验证码
            redisTemplate.delete(key);
            log.info("验证码验证成功: email={}", email);
            return true;
            
        } catch (Exception e) {
            log.error("验证码验证失败: email={}", email, e);
            return false;
        }
    }
    
    /**
     * 验证邮箱并激活用户
     */
    public boolean verifyEmailAndActivate(String email, String code) {
        // 1. 验证验证码
        if (!verifyCode(email, code)) {
            return false;
        }
        
        // 2. 更新用户的邮箱验证状态
        try {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", email);
            
            User user = userMapper.selectOne(queryWrapper);
            if (user == null) {
                log.error("用户不存在: email={}", email);
                return false;
            }
            
            // 更新邮箱验证状态
            user.setEmailVerified(true);
            userMapper.updateById(user);
            
            log.info("用户邮箱验证成功: userId={}, email={}", user.getId(), email);
            return true;
            
        } catch (Exception e) {
            log.error("激活用户失败: email={}", email, e);
            return false;
        }
    }
    
    /**
     * 检查验证码是否存在
     */
    public boolean hasVerificationCode(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * 获取验证码剩余时间（秒）
     */
    public long getCodeRemainingTime(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null && expire > 0 ? expire : 0;
    }
    
    /**
     * 生成随机验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String code) {
        return String.format(
            "您的验证码是: %s\n\n" +
            "验证码有效期为 %d 分钟，请尽快使用。\n" +
            "如果这不是您的操作，请忽略此邮件。",
            code, CODE_EXPIRE_MINUTES
        );
    }
}
