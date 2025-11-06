package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.model.User;
import com.example.domain.model.UserCredential;
import com.example.user.mapper.UserCredentialMapper;
import com.example.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务
 * 处理用户相关的业务逻辑
 */
@Slf4j
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserCredentialMapper userCredentialMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 根据邮箱获取用户详情（用于登录认证）
     */
    @Cacheable(value = "userDetails", key = "#email")
    public UserDetailsDTO getUserDetailsByEmail(String email) {
        try {
            UserDetailsDTO userDetails = userMapper.getUserDetailsByEmail(email);
            if (userDetails != null) {
                log.info("查询用户详情成功: email={}", email);
            } else {
                log.warn("用户不存在: email={}", email);
            }
            return userDetails;
        } catch (Exception e) {
            log.error("查询用户详情失败: email={}", email, e);
            return null;
        }
    }
    
    /**
     * 根据用户名获取用户详情
     */
    public UserDetailsDTO getUserDetailsByUsername(String username) {
        try {
            return userMapper.getUserDetailsByUsername(username);
        } catch (Exception e) {
            log.error("查询用户详情失败: username={}", username, e);
            return null;
        }
    }
    
    /**
     * 检查邮箱是否存在
     */
    public boolean checkEmailExists(String email) {
        try {
            int count = userMapper.checkEmailExists(email);
            return count > 0;
        } catch (Exception e) {
            log.error("检查邮箱失败: email={}", email, e);
            return false;
        }
    }
    
    /**
     * 检查用户名是否存在
     */
    public boolean checkUsernameExists(String username) {
        try {
            int count = userMapper.checkUsernameExists(username);
            return count > 0;
        } catch (Exception e) {
            log.error("检查用户名失败: username={}", username, e);
            return false;
        }
    }
    
    /**
     * 根据用户ID获取用户信息
     */
    public User getUserById(Long userId) {
        try {
            return userMapper.selectById(userId);
        } catch (Exception e) {
            log.error("查询用户失败: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 创建新用户（邮箱注册）
     */
    @Transactional(rollbackFor = Exception.class)
    public User createUser(String username, String email, String password) {
        try {
            // 1. 检查邮箱是否已存在
            if (checkEmailExists(email)) {
                log.warn("邮箱已存在: email={}", email);
                return null;
            }
            
            // 2. 创建用户
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            userMapper.insert(user);
            log.info("用户创建成功: userId={}, username={}, email={}", user.getId(), username, email);
            
            // 3. 创建用户凭证（密码）
            UserCredential credential = new UserCredential();
            credential.setUserId(user.getId());
            credential.setProvider("email");
            credential.setPasswordHash(passwordEncoder.encode(password));
            credential.setCreatedAt(LocalDateTime.now());
            
            userCredentialMapper.insert(credential);
            log.info("用户凭证创建成功: userId={}", user.getId());
            
            // 4. 发送欢迎邮件（异步）
            sendWelcomeEmail(email, username);
            
            return user;
            
        } catch (Exception e) {
            log.error("创建用户失败: username={}, email={}", username, email, e);
            throw new RuntimeException("创建用户失败", e);
        }
    }
    
    /**
     * 创建或更新第三方登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public User createOrUpdateOAuthUser(String provider, String providerUserId, 
                                       String username, String email, String avatarUrl) {
        try {
            // 1. 查找是否已存在
            User existingUser = userMapper.findByProviderAndProviderUserId(provider, providerUserId);
            
            if (existingUser != null) {
                // 更新用户信息
                existingUser.setUsername(username);
                existingUser.setEmail(email);
                existingUser.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(existingUser);
                
                log.info("更新第三方用户: userId={}, provider={}", existingUser.getId(), provider);
                return existingUser;
            }
            
            // 2. 创建新用户
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setEmailVerified(true); // 第三方登录默认邮箱已验证
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            userMapper.insert(user);
            log.info("创建第三方用户: userId={}, provider={}", user.getId(), provider);
            
            // 3. 创建用户凭证
            UserCredential credential = new UserCredential();
            credential.setUserId(user.getId());
            credential.setProvider(provider);
            credential.setProviderUserId(providerUserId);
            credential.setCreatedAt(LocalDateTime.now());
            
            userCredentialMapper.insert(credential);
            log.info("创建第三方凭证: userId={}, provider={}", user.getId(), provider);
            
            return user;
            
        } catch (Exception e) {
            log.error("创建或更新第三方用户失败: provider={}, providerUserId={}", provider, providerUserId, e);
            throw new RuntimeException("创建或更新第三方用户失败", e);
        }
    }
    
    /**
     * 更新用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        try {
            user.setUpdatedAt(LocalDateTime.now());
            int rows = userMapper.updateById(user);
            
            if (rows > 0) {
                log.info("更新用户成功: userId={}", user.getId());
                return true;
            }
            
            log.warn("更新用户失败: userId={}", user.getId());
            return false;
            
        } catch (Exception e) {
            log.error("更新用户失败: userId={}", user.getId(), e);
            return false;
        }
    }
    
    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        try {
            // 1. 验证旧密码
            UserCredential credential = userCredentialMapper.findByUserIdAndProvider(userId, "email");
            if (credential == null) {
                log.warn("用户凭证不存在: userId={}", userId);
                return false;
            }
            
            if (!passwordEncoder.matches(oldPassword, credential.getPasswordHash())) {
                log.warn("旧密码错误: userId={}", userId);
                return false;
            }
            
            // 2. 更新密码
            String newPasswordHash = passwordEncoder.encode(newPassword);
            int rows = userCredentialMapper.updatePassword(userId, newPasswordHash);
            
            if (rows > 0) {
                log.info("修改密码成功: userId={}", userId);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("修改密码失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 发送欢迎邮件
     */
    private void sendWelcomeEmail(String email, String username) {
        try {
            Map<String, String> emailData = new HashMap<>();
            emailData.put("to", email);
            emailData.put("subject", "欢迎注册");
            emailData.put("content", String.format("欢迎 %s 注册我们的平台！", username));
            
            // 使用交换机和路由键发送消息
            rabbitTemplate.convertAndSend("user.exchange", "email.welcome", emailData);
            log.info("欢迎邮件已发送到交换机: email={}, exchange=user.exchange, routingKey=email.welcome", email);
            
        } catch (Exception e) {
            log.error("发送欢迎邮件失败: email={}", email, e);
        }
    }
}
