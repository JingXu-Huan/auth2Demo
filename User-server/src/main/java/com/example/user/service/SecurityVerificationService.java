package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.model.User;
import com.example.user.mapper.UserMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 安全验证服务
 * 处理长时间未登录的账号安全验证
 */
@Service
public class SecurityVerificationService {

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 自动装配用户Mapper
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 自动装配RabbitTemplate
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 自动装配RedisTemplate
     */
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * Redis key前缀
     */
    private static final String SECURITY_CODE_PREFIX = "security:code:";

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 验证码有效期（分钟）
     */
    private static final int CODE_EXPIRY_MINUTES = 5;

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 长时间未登录的天数阈值（30天）
     */
    private static final int INACTIVE_DAYS_THRESHOLD = 30;

    /**
     * 60秒不能重复获取验证码
     */
    private static final String NOT_SEND_EMAIL = "Not:email:";

    /**
     * @param email 用户邮箱
     * @return true 需要验证，false 不需要
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 检查用户是否需要安全验证
     */
    public boolean needsSecurityVerification(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return false;
        }

        // 如果从未登录过，不需要额外验证
        if (user.getLastLoginAt() == null) {
            return false;
        }

        // 计算距离上次登录的天数
        LocalDateTime lastLogin = user.getLastLoginAt();
        LocalDateTime now = LocalDateTime.now();
        long daysSinceLastLogin = java.time.Duration.between(lastLogin, now).toDays();

        // 如果超过阈值，需要验证
        return daysSinceLastLogin > INACTIVE_DAYS_THRESHOLD;
    }

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 获取距离上次登录的天数
     */
    public long getDaysSinceLastLogin(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);

        if (user == null || user.getLastLoginAt() == null) {
            return 0;
        }

        return java.time.Duration.between(user.getLastLoginAt(), LocalDateTime.now()).toDays();
    }

    /**
     * @param email 用户邮箱
     * @return 验证码发送结果
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 发送安全验证码
     */
    public Map<String, Object> sendSecurityCode(String email) {
        Map<String, Object> result = new HashMap<>();
        // 60秒防刷机制
        Boolean set = redisTemplate.opsForValue()
                .setIfAbsent(NOT_SEND_EMAIL + email, "", 60, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(set)) {
            // 获取TTL并返回
            Long ttl = redisTemplate.getExpire(NOT_SEND_EMAIL + email);
            ttl = (ttl == null || ttl < 0) ? 0 : ttl;
            result.put("success", false);
            result.put("message", "请等待" + ttl + "秒再尝试获取新的验证码");
            return result;
        }
        try {
            // 生成6位数字验证码
            String code = generateVerificationCode();
            // 保存验证码到 Redis（5分钟有效期）
            String redisKey = SECURITY_CODE_PREFIX + email;
            redisTemplate.opsForValue().set(redisKey, code, CODE_EXPIRY_MINUTES, TimeUnit.MINUTES);

            // 发送邮件（使用 EmailVerificationService）
            String subject = "安全验证码";
            String content = String.format(
                    "<h2>账号安全验证</h2>" +
                            "<p>您好，</p>" +
                            "<p>检测到您的账号长时间未登录，为了保障账号安全，请输入以下验证码完成登录：</p>" +
                            "<h1 style='color: #1a73e8; font-size: 32px; letter-spacing: 5px;'>%s</h1>" +
                            "<p>验证码有效期为 <strong>5分钟</strong>。</p>" +
                            "<p>如果这不是您本人的操作，请立即修改密码。</p>" +
                            "<hr>" +
                            "<p style='color: #666; font-size: 12px;'>此邮件由系统自动发送，请勿回复。</p>",
                    code
            );
            // 通过 RabbitMQ 发送邮件
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("to", email);
            emailData.put("subject", subject);
            emailData.put("content", content);
            rabbitTemplate.convertAndSend("email.exchange", "email.send", emailData);

            result.put("success", true);
            result.put("message", "验证码已发送到您的邮箱");
            result.put("expiresIn", 300); // 5分钟 = 300秒
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送验证码失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * @param email 用户邮箱
     * @param code  验证码
     * @return 验证结果
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 验证安全验证码
     */
    public boolean verifySecurityCode(String email, String code) {
        String redisKey = SECURITY_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            return false;
        }

        // 验证码匹配
        boolean isValid = storedCode.equals(code);

        if (isValid) {
            // 验证成功后删除验证码
            redisTemplate.delete(redisKey);

            // 更新用户最后登录时间
            updateLastLoginTime(email);
        }

        return isValid;
    }

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 更新用户最后登录时间
     */
    private void updateLastLoginTime(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);

        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 生成6位数字验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
