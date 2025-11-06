package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.user.mapper.UserMapper;
import com.example.common.model.User;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 邮箱验证服务
 * 处理邮箱验证相关的业务逻辑
 */
@Slf4j
@Service
public class EmailVerificationService {
    
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
     * @version 2.0.0
     * @date 2025-11-06
     * 重新发送验证邮件（通过邮箱地址）
     * @param email 邮箱地址
     * @return Map<String, Object> 结果
     */
    @ApiOperation(value = "重新发送验证邮件（通过邮箱地址）")
    public Map<String, Object> resendVerificationEmailByEmail(String email) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 查询用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            result.put("success", false);
            result.put("message", "该邮箱未注册");
            return result;
        }
        
        // 检查是否已验证
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            result.put("success", false);
            result.put("message", "邮箱已验证，无需重复验证");
            return result;
        }
        
        // 生成新的验证令牌
        String verificationToken = UUID.randomUUID().toString();
        user.setConfirmationToken(verificationToken);
        user.setTokenExpiry(LocalDateTime.now().plusHours(24)); // 24小时有效
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 构建验证链接
        String verificationLink = "http://localhost:5173/verify-email?token=" + verificationToken;
        
        // 发送验证邮件
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", email);
        emailData.put("subject", "【重新发送】请验证您的邮箱");
        emailData.put("content", String.format(
            "<h2>邮箱验证</h2>" +
            "<p>您好，%s！</p>" +
            "<p>您请求重新发送验证邮件。请点击下面的链接验证您的邮箱：</p>" +
            "<p><a href='%s' style='display: inline-block; padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px;'>验证邮箱</a></p>" +
            "<p>或复制以下链接到浏览器：</p>" +
            "<p>%s</p>" +
            "<p>此链接将在24小时后失效。</p>" +
            "<p style='color: #666; font-size: 12px;'>如果这不是您本人的操作，请忽略此邮件。</p>",
            user.getUsername(), verificationLink, verificationLink
        ));
        
        rabbitTemplate.convertAndSend("email.exchange", "email.send", emailData);
        
        result.put("success", true);
        result.put("message", "验证邮件已重新发送");
        result.put("email", email);
        return result;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 重新发送验证邮件（通过 Token）
     * @deprecated 建议使用 resendVerificationEmailByEmail(String email)
     */
    @Deprecated
    @ApiOperation(value = "重新发送验证邮件（通过 Token）")
    public Map<String, Object> resendVerificationEmail(String token) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 解析 token 获取用户信息（简化处理）
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            result.put("success", false);
            result.put("message", "无效的令牌");
            return result;
        }
        
        // 解码 payload（实际应该使用 JWT 库）
        String payload = new String(Base64Utils.decodeFromUrlSafeString(parts[1]), StandardCharsets.UTF_8);
        // 这里简化处理，实际应该用 Jackson 解析 JSON
        String email = extractEmailFromPayload(payload);
        
        if (email == null) {
            result.put("success", false);
            result.put("message", "无法从令牌中获取邮箱");
            return result;
        }
        
        // 查询用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        
        // 检查是否已验证
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            result.put("success", false);
            result.put("message", "邮箱已验证，无需重复验证");
            return result;
        }
        
        // 生成新的验证令牌
        String verificationToken = UUID.randomUUID().toString();
        user.setConfirmationToken(verificationToken);
        user.setTokenExpiry(LocalDateTime.now().plusHours(24)); // 24小时有效
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 发送验证邮件（通过消息队列）
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("email", email);
        emailData.put("username", user.getUsername());
        emailData.put("token", verificationToken);
        emailData.put("type", "verification");
        
        rabbitTemplate.convertAndSend("email.exchange", "email.verification", emailData);
        
        log.info("重新发送验证邮件给用户: {}", email);
        
        result.put("success", true);
        result.put("email", email);
        result.put("message", "验证邮件已发送");
        return result;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 获取邮箱验证状态
     */
    public Map<String, Object> getVerificationStatus(String token) throws Exception {
        Map<String, Object> status = new HashMap<>();
        
        // 解析 token（简化处理）
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            status.put("valid", false);
            status.put("message", "无效的令牌");
            return status;
        }
        
        String payload = new String(Base64Utils.decodeFromUrlSafeString(parts[1]), StandardCharsets.UTF_8);
        String email = extractEmailFromPayload(payload);
        
        if (email == null) {
            status.put("valid", false);
            status.put("message", "无法从令牌中获取邮箱");
            return status;
        }
        
        // 查询用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            status.put("valid", false);
            status.put("message", "用户不存在");
            return status;
        }
        
        status.put("valid", true);
        status.put("email", email);
        status.put("emailVerified", user.getEmailVerified());
        status.put("username", user.getUsername());
        
        // 如果有待验证的令牌，检查是否过期
        if (user.getConfirmationToken() != null && user.getTokenExpiry() != null) {
            boolean tokenExpired = user.getTokenExpiry().isBefore(LocalDateTime.now());
            status.put("hasPendingToken", true);
            status.put("tokenExpired", tokenExpired);
        } else {
            status.put("hasPendingToken", false);
        }
        
        return status;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 从 JWT payload 中提取邮箱（简化实现）
     * 实际应该使用 Jackson 解析 JSON
     */
    private String extractEmailFromPayload(String payload) {
        try {
            // 查找 "email":"xxx" 的模式
            int emailIndex = payload.indexOf("\"email\":");
            if (emailIndex == -1) return null;
            
            int startQuote = payload.indexOf("\"", emailIndex + 8);
            int endQuote = payload.indexOf("\"", startQuote + 1);
            
            if (startQuote != -1 && endQuote != -1) {
                return payload.substring(startQuote + 1, endQuote);
            }
        } catch (Exception e) {
            log.error("解析邮箱失败", e);
        }
        return null;
    }
}
