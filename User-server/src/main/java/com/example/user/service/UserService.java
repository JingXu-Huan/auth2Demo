package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.dto.UserDetailsDTO;
import com.example.common.model.User;
import com.example.common.model.UserCredential;
import com.example.user.mapper.UserCredentialMapper;
import com.example.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户服务
 */
@Slf4j
@Service
public class UserService {
    
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
     * 自动装配用户凭证Mapper
     */
    @Autowired
    private UserCredentialMapper credentialMapper;

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
     * 自动装配PasswordEncoder
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 根据用户名查询用户详情（包含凭证信息）
     */
    public UserDetailsDTO getUserDetailsByUsername(String username) {
        return userMapper.findUserDetailsByUsername(username);
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 根据邮箱查询用户详情（包含凭证信息）
     */
    public UserDetailsDTO getUserDetailsByEmail(String email) {
        return userMapper.findUserDetailsByEmail(email);
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 根据用户名查询用户
     */
    public User getUserByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return userMapper.selectOne(wrapper);
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 根据ID查询用户
     */
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 检查用户名是否存在
     */
    public boolean usernameExists(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        Long count = userMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 检查邮箱是否存在
     */
    public boolean emailExists(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        Long count = userMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 验证密码是否正确
     */
    public boolean validatePassword(String email, String password) {
        UserDetailsDTO userDetails = getUserDetailsByEmail(email);
        
        if (userDetails == null || userDetails.getPasswordHash() == null) {
            return false;
        }
        
        return passwordEncoder.matches(password, userDetails.getPasswordHash());
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 保存或更新 Gitee 用户
     */
    @Transactional
    public User saveOrUpdateGiteeUser(String giteeUserId, String login, String name, 
                                      String email, String avatarUrl) {
        // 1. 查询是否已存在该 Gitee 用户的凭证
        QueryWrapper<UserCredential> credentialWrapper = new QueryWrapper<>();
        credentialWrapper.eq("provider", "gitee")
                        .eq("provider_user_id", giteeUserId);
        UserCredential credential = credentialMapper.selectOne(credentialWrapper);
        
        User user;
        
        if (credential != null) {
            // 已存在，更新用户信息
            user = userMapper.selectById(credential.getUserId());
            user.setDisplayName(name);
            user.setEmail(email);
            user.setAvatarUrl(avatarUrl);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        } else {
            // 新用户，创建用户记录
            user = new User();
            user.setUsername(login);  // 使用 Gitee 的 login 作为 username
            user.setDisplayName(name);
            user.setEmail(email);
            user.setEmailVerified(email != null);
            user.setAvatarUrl(avatarUrl);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
            
            // 创建 Gitee 凭证记录
            UserCredential newCredential = new UserCredential();
            newCredential.setUserId(user.getId());
            newCredential.setProvider("gitee");
            newCredential.setProviderUserId(giteeUserId);
            newCredential.setPasswordHash(null);  // Gitee 登录不需要密码
            newCredential.setCreatedAt(LocalDateTime.now());
            credentialMapper.insert(newCredential);
        }
        
        return user;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 创建新用户（邮箱密码方式）
     */
    @Transactional
    public User createUser(String username, String email, String passwordHash) {
        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        //创建token
        String token= UUID.randomUUID().toString();
        //设置过期时间为12h
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(12);
        user.setConfirmationToken(token);
        user.setDisplayName("user");
        user.setTokenExpiry(expiryTime);
        //设置最后登录时间为30天前
        user.setLastLoginAt(LocalDateTime.now().minusDays(30));
        userMapper.insert(user);
        
        // 创建凭证
        UserCredential credential = new UserCredential();
        credential.setUserId(user.getId());
        credential.setProvider("email");
        credential.setProviderUserId(email);
        // 使用 BCrypt 加密密码
        credential.setPasswordHash(passwordEncoder.encode(passwordHash));
        credential.setCreatedAt(LocalDateTime.now());
        credentialMapper.insert(credential);

        // 构建验证链接
        String verificationLink = "http://localhost:5173/verify-email?token=" + token;
        
        // 发送验证邮件
        java.util.Map<String, Object> emailData = new java.util.HashMap<>();
        emailData.put("to", email);
        emailData.put("subject", "请验证您的邮箱");
        emailData.put("content", String.format(
            "<h2>欢迎注册！</h2>" +
            "<p>您好，%s！</p>" +
            "<p>感谢您的注册。请点击下面的链接验证您的邮箱：</p>" +
            "<p><a href='%s' style='display: inline-block; padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px;'>验证邮箱</a></p>" +
            "<p>或复制以下链接到浏览器：</p>" +
            "<p>%s</p>" +
            "<p>此链接将在12小时后失效。</p>" +
            "<p style='color: #666; font-size: 12px;'>如果这不是您本人的操作，请忽略此邮件。</p>",
            username, verificationLink, verificationLink
        ));
        
        rabbitTemplate.convertAndSend("email.exchange", "email.send", emailData);

        return user;
    }

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 确认邮箱验证 Token,实现注册.
     */
    @Transactional
    public ResponseEntity<String> confirmToken(String token) {
        // 1. 根据 confirmation_token 查询用户
        QueryWrapper<User> wrapper = new QueryWrapper<User>().eq("confirmation_token", token);
        User user = userMapper.selectOne(wrapper);

        // 2. Token 不存在
        if (user == null) {
            return ResponseEntity.badRequest().body("链接无效或已失效");
        }
        
        // 3. 检查 Token 是否已经使用过（邮箱已验证）
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            return ResponseEntity.badRequest().body("该账户已经激活，请直接登录");
        }

        // 4. 检查过期时间（12小时内有效）
        LocalDateTime expiryTime = user.getTokenExpiry();
        LocalDateTime now = LocalDateTime.now();

        if (expiryTime == null || expiryTime.isBefore(now)) {
            // 令牌已过期
            return ResponseEntity.badRequest().body("验证链接已过期，请重新注册");
        }
        
        // 5. 验证通过，激活用户账户
        user.setConfirmationToken(null);     // 清除 token
        user.setTokenExpiry(null);          // 清除过期时间
        user.setEmailVerified(true);         // 标记邮箱已验证
        user.setUpdatedAt(LocalDateTime.now());
        
        log.info("准备更新用户 - ID: {}, 清除 token 和过期时间", user.getId());
        
        // 更新用户（配置了 update-strategy: ignored，会更新 null 值）
        int updated = userMapper.updateById(user);
        
        log.info("用户更新完成 - ID: {}, 影响行数: {}", user.getId(), updated);
        
        // 6. 返回成功响应（可以返回 HTML 或重定向）
        return ResponseEntity.ok().body("邮箱验证成功！您的账户已激活，请前往登录。");
    }
}
