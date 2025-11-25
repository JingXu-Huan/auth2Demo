package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.model.User;
import com.example.domain.model.UserProfile;
import com.example.domain.model.PasswordHistory;
import com.example.user.mapper.UserMapper;
import com.example.user.mapper.PasswordHistoryMapper;
import com.example.user.validator.PasswordValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户核心服务
 * 基于新数据库设计，处理用户的基本信息管理
 * 
 * @author System
 * @since 2024-11-25
 */
@Slf4j
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordHistoryMapper passwordHistoryMapper;
    
    @Autowired
    private UserProfileService userProfileService;
    
    @Autowired
    private EmailVerificationService emailVerificationService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private PasswordValidator passwordValidator;
    
    private static final int PASSWORD_HISTORY_LIMIT = 5;
    
    /**
     * 根据邮箱获取用户详情（用于登录认证）
     */
    @Cacheable(value = "userDetails", key = "#email")
    public UserDetailsDTO getUserDetailsByEmail(String email) {
        try {
            User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("email", email)
            );
            
            if (user == null) {
                log.debug("用户不存在: email={}", email);
                return null;
            }
            
            // 构建用户详情DTO
            UserDetailsDTO dto = new UserDetailsDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setPasswordHash(user.getPasswordHash());
            dto.setEmailVerified(user.getEmailVerified());
            
            // 获取用户详细资料
            UserProfile profile = userProfileService.getUserProfile(user.getId());
            if (profile != null) {
                dto.setDisplayName(profile.getNickname());
                dto.setAvatarUrl(profile.getAvatarUrl());
            }
            
            return dto;
        } catch (Exception e) {
            log.error("获取用户详情失败: email={}", email, e);
            return null;
        }
    }
    
    /**
     * 根据用户ID获取用户详情
     */
    @Cacheable(value = "userDetails", key = "'id:' + #userId")
    public UserDetailsDTO getUserDetailsById(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return null;
            }
            
            UserDetailsDTO dto = new UserDetailsDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setPasswordHash(user.getPasswordHash());
            dto.setEmailVerified(user.getEmailVerified());
            
            // 获取用户详细资料
            UserProfile profile = userProfileService.getUserProfile(userId);
            if (profile != null) {
                dto.setDisplayName(profile.getNickname());
                dto.setAvatarUrl(profile.getAvatarUrl());
            }
            
            return dto;
        } catch (Exception e) {
            log.error("获取用户详情失败: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 根据用户名获取用户详情
     */
    @Cacheable(value = "userDetails", key = "'username:' + #username")
    public UserDetailsDTO getUserDetailsByUsername(String username) {
        try {
            User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("username", username)
            );
            
            if (user == null) {
                return null;
            }
            
            return getUserDetailsById(user.getId());
        } catch (Exception e) {
            log.error("获取用户详情失败: username={}", username, e);
            return null;
        }
    }
    
    /**
     * 检查邮箱是否存在
     */
    public boolean checkEmailExists(String email) {
        try {
            Long count = userMapper.selectCount(
                new QueryWrapper<User>().eq("email", email)
            );
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
            Long count = userMapper.selectCount(
                new QueryWrapper<User>().eq("username", username)
            );
            return count > 0;
        } catch (Exception e) {
            log.error("检查用户名失败: username={}", username, e);
            return false;
        }
    }
    
    /**
     * 更新用户最后登录时间
     */
    @CacheEvict(value = "userDetails", allEntries = true)
    public void updateLastLoginTime(String email) {
        try {
            User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("email", email)
            );
            
            if (user != null) {
                user.setLastLoginAt(LocalDateTime.now());
                userMapper.updateById(user);
                log.info("更新最后登录时间成功: email={}", email);
            }
        } catch (Exception e) {
            log.error("更新最后登录时间失败: email={}", email, e);
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
     * 根据邮箱获取用户信息
     */
    public User getUserByEmail(String email) {
        try {
            return userMapper.selectOne(
                new QueryWrapper<User>().eq("email", email)
            );
        } catch (Exception e) {
            log.error("查询用户失败: email={}", email, e);
            return null;
        }
    }
    
    /**
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
            user.setPasswordHash(passwordEncoder.encode(password)); // 新数据库设计：密码直接存在users表
            user.setEmailVerified(false);
            user.setStatus("active");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            userMapper.insert(user);
            log.info("用户创建成功: userId={}, username={}, email={}", user.getId(), username, email);
            
            // 4. 创建默认用户详情
            userProfileService.getUserProfile(user.getId()); // 会自动创建默认profile
            
            // 5. 记录密码历史
            savePasswordHistory(user.getId(), user.getPasswordHash());
            
            // 6. 发送验证邮件
            try {
                emailVerificationService.sendVerificationCode(email);
                log.info("验证邮件已发送: email={}", email);
            } catch (Exception e) {
                log.warn("发送验证邮件失败，但用户已创建: email={}", email, e);
                // 不影响注册流程
            }
            
            return user;
            
        } catch (Exception e) {
            log.error("创建用户失败: username={}, email={}", username, email, e);
            throw new RuntimeException("创建用户失败", e);
        }
    }
    
    /**
     * 记录密码历史
     */
    @Async
    private void savePasswordHistory(Long userId, String passwordHash) {
        try {
            PasswordHistory history = new PasswordHistory();
            history.setUserId(userId);
            history.setPasswordHash(passwordHash);
            history.setCreatedAt(LocalDateTime.now());
            passwordHistoryMapper.insert(history);
            
            // 删除超过限制的旧记录
            List<PasswordHistory> histories = passwordHistoryMapper.selectList(
                new QueryWrapper<PasswordHistory>()
                    .eq("user_id", userId)
                    .orderByDesc("created_at")
            );
            
            if (histories.size() > PASSWORD_HISTORY_LIMIT) {
                for (int i = PASSWORD_HISTORY_LIMIT; i < histories.size(); i++) {
                    passwordHistoryMapper.deleteById(histories.get(i).getId());
                }
            }
        } catch (Exception e) {
            log.error("保存密码历史失败: userId={}", userId, e);
        }
    }
    
    /**
     * 创建或更新OAuth用户（由OAuthService处理）
     */
    @Deprecated
    public User createOrUpdateOAuthUser(String provider, String providerUserId,
                                       String username, String email, String avatarUrl) {
        // 此功能已迁移到 OAuthService
        log.warn("调用已废弃的方法createOrUpdateOAuthUser，请使用OAuthService");
        return null;
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
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "userDetails", allEntries = true)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return false;
            }
            
            // 1. 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                log.warn("旧密码错误: userId={}", userId);
                return false;
            }
            
            // 2. 验证新密码强度
            String passwordError = passwordValidator.validate(newPassword);
            if (passwordError != null) {
                log.warn("新密码强度不足: userId={}, error={}", userId, passwordError);
                throw new IllegalArgumentException(passwordError);
            }
            
            // 3. 检查密码历史
            if (checkPasswordHistory(userId, newPassword)) {
                log.warn("密码与最近使用的密码重复: userId={}", userId);
                throw new IllegalArgumentException("不能使用最近使用过的密码");
            }
            
            // 4. 更新密码
            String newPasswordHash = passwordEncoder.encode(newPassword);
            user.setPasswordHash(newPasswordHash);
            user.setUpdatedAt(LocalDateTime.now());
            int rows = userMapper.updateById(user);
            
            if (rows > 0) {
                // 记录密码历史
                savePasswordHistory(userId, newPasswordHash);
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
     * 检查密码历史
     */
    private boolean checkPasswordHistory(Long userId, String newPassword) {
        try {
            List<PasswordHistory> histories = passwordHistoryMapper.selectList(
                new QueryWrapper<PasswordHistory>()
                    .eq("user_id", userId)
                    .orderByDesc("created_at")
                    .last("LIMIT " + PASSWORD_HISTORY_LIMIT)
            );
            
            for (PasswordHistory history : histories) {
                if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("检查密码历史失败: userId={}", userId, e);
            return false;
        }
    }
    
}
