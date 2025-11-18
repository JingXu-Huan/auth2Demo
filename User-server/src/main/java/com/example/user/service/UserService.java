package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.model.User;
import com.example.domain.model.UserCredential;
import com.example.user.feign.RelationshipUserClient;
import com.example.user.mapper.UserCredentialMapper;
import com.example.user.mapper.UserMapper;
import com.example.user.validator.PasswordValidator;
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
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
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
    
    @Autowired
    private PasswordValidator passwordValidator;
    
    @Autowired
    private RelationshipUserClient relationshipUserClient;
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 根据邮箱获取用户详情（用于登录认证）
     */
    public UserDetailsDTO getUserDetailsByEmail(String email) {
        try {
            log.info("开始查询用户详情: email={}", email);
            
            // 调试：先查用户
            User user = userMapper.getUserByEmail(email);
            if (user == null) {
                log.warn("用户不存在: email={}", email);
                return null;
            }
            log.info("找到用户: userId={}, email={}", user.getId(), email);
            
            // 调试：查询凭证
            UserCredential credential = userCredentialMapper.findByUserIdAndProvider(user.getId(), "email");
            if (credential == null) {
                log.warn("用户凭证不存在: userId={}, provider=email", user.getId());
            } else {
                log.info("找到用户凭证: credentialId={}, userId={}, provider={}, passwordHash={}", 
                    credential.getId(), credential.getUserId(), credential.getProvider(),
                    credential.getPasswordHash() != null ? "存在(长度:" + credential.getPasswordHash().length() + ")" : "NULL");
            }
            
            // 正常查询
            UserDetailsDTO userDetails = userMapper.getUserDetailsByEmail(email);
            if (userDetails != null) {
                log.info("查询用户详情成功: userId={}, passwordHash={}", 
                    userDetails.getUserId(),
                    userDetails.getPasswordHash() != null ? "存在(长度:" + userDetails.getPasswordHash().length() + ")" : "NULL");
            } else {
                log.warn("getUserDetailsByEmail 返回 null: email={}", email);
            }
            return userDetails;
        } catch (Exception e) {
            log.error("查询用户详情失败: email={}", email, e);
            return null;
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
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
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
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
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
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
     * 更新用户最后登录时间
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
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
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
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 创建新用户（邮箱注册）
     */
    @Transactional(rollbackFor = Exception.class)
    public User createUser(String username, String email, String password) {
        try {
            // 1. 验证密码强度
            String passwordError = passwordValidator.validate(password);
            if (passwordError != null) {
                log.warn("密码强度不足: email={}, error={}", email, passwordError);
                throw new IllegalArgumentException(passwordError);
            }
            
            // 2. 检查邮箱是否已存在
            if (checkEmailExists(email)) {
                log.warn("邮箱已存在: email={}", email);
                return null;
            }
            
            // 3. 创建用户
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            userMapper.insert(user);
            log.info("用户创建成功: userId={}, username={}, email={}", user.getId(), username, email);
            
            // 3. 创建用户凭证（密码）
            log.info("准备创建用户凭证: userId={}, provider=email", user.getId());
            UserCredential credential = new UserCredential();
            credential.setUserId(user.getId());
            credential.setProvider("email");
            String encodedPassword = passwordEncoder.encode(password);
            credential.setPasswordHash(encodedPassword);
            credential.setCreatedAt(LocalDateTime.now());
            
            log.info("密码已加密: userId={}, passwordHashLength={}", user.getId(), encodedPassword != null ? encodedPassword.length() : 0);
            
            int insertRows = userCredentialMapper.insert(credential);
            log.info("用户凭证创建成功: userId={}, insertRows={}, credentialId={}", 
                user.getId(), insertRows, credential.getId());
            
            // 4. 发送欢迎邮件（异步）
            sendWelcomeEmail(email, username);

            // 5. 在 Neo4j 中初始化用户节点（关系服务）
            try {
                relationshipUserClient.ensureUserNode(user.getId());
                log.info("已通知关系服务在 Neo4j 中初始化用户节点: userId={}", user.getId());
            } catch (Exception e) {
                log.warn("通知关系服务初始化 Neo4j 用户节点失败: userId={}", user.getId(), e);
            }
            
            return user;
            
        } catch (Exception e) {
            log.error("创建用户失败: username={}, email={}", username, email, e);
            throw new RuntimeException("创建用户失败", e);
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
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

                try {
                    relationshipUserClient.ensureUserNode(existingUser.getId());
                    log.info("已通知关系服务在 Neo4j 中初始化用户节点(第三方用户-更新): userId={}", existingUser.getId());
                } catch (Exception e) {
                    log.warn("通知关系服务初始化 Neo4j 用户节点失败(第三方用户-更新): userId={}", existingUser.getId(), e);
                }

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

            try {
                relationshipUserClient.ensureUserNode(user.getId());
                log.info("已通知关系服务在 Neo4j 中初始化用户节点(第三方用户-创建): userId={}", user.getId());
            } catch (Exception e) {
                log.warn("通知关系服务初始化 Neo4j 用户节点失败(第三方用户-创建): userId={}", user.getId(), e);
            }

            return user;
            
        } catch (Exception e) {
            log.error("创建或更新第三方用户失败: provider={}, providerUserId={}", provider, providerUserId, e);
            throw new RuntimeException("创建或更新第三方用户失败", e);
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
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
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
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
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
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
    
    /**
     * 根据邮箱获取用户
     */
    public User getUserByEmail(String email) {
        return userMapper.getUserByEmail(email);
    }
}
