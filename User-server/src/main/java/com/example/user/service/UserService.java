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

import org.springframework.data.redis.core.RedisTemplate;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * ====================================================================
 * 用户核心服务 (User Service)
 * ====================================================================
 * 
 * 【服务职责】
 * 本服务是User-server模块的核心，负责用户相关的业务逻辑：
 * - 用户信息的CRUD操作
 * - 用户注册与邮箱验证
 * - 密码管理（加密、修改、历史记录）
 * - 登录认证信息查询
 * 
 * 【分层架构中的位置】
 * Controller → Service（本类） → Mapper → Database
 *              ↓
 *         业务逻辑处理
 *         事务管理
 *         缓存处理
 *         调用其他Service
 * 
 * 【核心注解说明】
 * @Service - 标记为Spring服务组件，会被自动扫描并注册为Bean
 * @Slf4j   - Lombok注解，自动生成 log 日志对象
 * 
 * 【依赖注入】
 * 本类使用 @Autowired 字段注入（简单但不推荐）
 * 更好的方式是构造器注入（参考RoleController）
 * 
 * 【关键设计模式】
 * 1. 缓存策略：使用Spring Cache + Redis缓存用户信息
 * 2. 密码安全：BCrypt加密 + 密码历史防重复
 * 3. 软删除：使用deleted_at字段标记删除
 * 
 * @author System
 * @since 2024-11-25
 * @see UserMapper 用户数据访问层
 * @see UserProfileService 用户资料服务
 */
@Slf4j
@Service
public class UserService {
    
    /** 用户数据访问对象 - 操作users表 */
    @Autowired
    private UserMapper userMapper;
    
    /** 密码历史记录访问对象 - 防止使用最近用过的密码 */
    @Autowired
    private PasswordHistoryMapper passwordHistoryMapper;
    
    /** 用户资料服务 - 管理用户详细信息 */
    @Autowired
    private UserProfileService userProfileService;
    
    /** 邮箱验证服务 - 处理邮箱验证码 */
    @Autowired
    private EmailVerificationService emailVerificationService;
    
    /**
     * 密码编码器 - 使用BCrypt算法
     * 
     * 【BCrypt特点】
     * - 自动加盐，每次结果不同
     * - 可配置迭代次数（默认10轮）
     * - 单向加密，无法解密
     */
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /** 密码强度验证器 - 检查密码复杂度 */
    @Autowired
    private PasswordValidator passwordValidator;
    
    /**
     * Redis模板 - 用于缓存操作
     * 
     * 【缓存策略】
     * - 用户信息缓存Key: user:{userId}
     * - 缓存过期时间: 通常1小时
     * - 写操作时清除缓存
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /** 密码历史记录限制 - 不能使用最近5次用过的密码 */
    private static final int PASSWORD_HISTORY_LIMIT = 5;
    
    /**
     * 根据邮箱获取用户详情（用于登录认证）
     */
    @Cacheable(value = "userDetails", key = "#p0", unless = "#result == null")
    public UserDetailsDTO getUserDetailsByEmail(String email) {
        log.info("======== UserService.getUserDetailsByEmail 开始 ========");
        log.info("查询邮箱: {}", email);
        
        try {
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("email", email)
                    .isNull("deleted_at")  // 排除已软删除的用户
            );
            
            if (user == null) {
                log.warn("用户不存在或已删除: email={}", email);
                return null;
            }
            
            log.info("数据库查询成功: userId={}, username={}, email={}", 
                user.getId(), user.getUsername(), user.getEmail());
            log.info("用户状态: status={}, emailVerified={}, mfaEnabled={}", 
                user.getStatus(), user.getEmailVerified(), user.getMfaEnabled());
            log.info("密码哈希: passwordHash={}", 
                user.getPasswordHash() != null ? "存在(长度:" + user.getPasswordHash().length() + ")" : "NULL");
            
            // 构建用户详情DTO
            UserDetailsDTO dto = new UserDetailsDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setPasswordHash(user.getPasswordHash());
            dto.setEmailVerified(user.getEmailVerified());
            
            log.info("DTO构建完成: userId={}, passwordHash={}, emailVerified={}", 
                dto.getUserId(), 
                dto.getPasswordHash() != null ? "存在(长度:" + dto.getPasswordHash().length() + ")" : "NULL",
                dto.getEmailVerified());
            
            // 获取用户详细资料
            try {
                UserProfile profile = userProfileService.getUserProfile(user.getId());
                if (profile != null) {
                    dto.setDisplayName(profile.getNickname());
                    dto.setAvatarUrl(profile.getAvatarUrl());
                }
            } catch (Exception profileEx) {
                log.warn("获取用户资料失败，忽略: {}", profileEx.getMessage());
            }
            
            log.info("======== UserService.getUserDetailsByEmail 完成 ========");
            return dto;
        } catch (Exception e) {
            log.error("获取用户详情失败: email={}", email, e);
            return null;
        }
    }
    
    /**
     * 根据用户ID获取用户详情
     */
    @Cacheable(value = "userDetails", key = "'id:' + #p0", unless = "#result == null")
    public UserDetailsDTO getUserDetailsById(Long userId) {
        try {
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("id", userId)
                    .isNull("deleted_at")  // 排除已软删除的用户
            );
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
    @Cacheable(value = "userDetails", key = "'username:' + #p0", unless = "#result == null")
    public UserDetailsDTO getUserDetailsByUsername(String username) {
        try {
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
                    .isNull("deleted_at")  // 排除已软删除的用户
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
                new QueryWrapper<User>()
                    .eq("email", email)
                    .isNull("deleted_at")  // 排除已软删除的用户
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
                new QueryWrapper<User>()
                    .eq("username", username)
                    .isNull("deleted_at")  // 排除已软删除的用户
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
                new QueryWrapper<User>()
                    .eq("email", email)
                    .isNull("deleted_at")
            );
            
            if (user != null) {
                user.setLastLoginAt(OffsetDateTime.now());
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
            return userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("id", userId)
                    .isNull("deleted_at")
            );
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
                new QueryWrapper<User>()
                    .eq("email", email)
                    .isNull("deleted_at")
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
            user.setEmailVerified(true);  // 注册即验证通过
            user.setStatus((short) 1);  // 1=active
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            
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
            
            // 7. 清除可能存在的旧缓存（防止缓存脏数据）
            evictUserCache(email);
            
            return user;
            
        } catch (Exception e) {
            log.error("创建用户失败: username={}, email={}", username, email, e);
            throw new RuntimeException("创建用户失败", e);
        }
    }
    
    /**
     * 清除用户缓存
     * 在用户信息变更时调用，确保下次读取时获取最新数据
     */
    public void evictUserCache(String email) {
        try {
            // Spring Cache 使用的 key 格式: cacheName::key
            String cacheKey = "userDetails::" + email;
            Boolean deleted = redisTemplate.delete(cacheKey);
            log.info("清除用户缓存: key={}, deleted={}", cacheKey, deleted);
        } catch (Exception e) {
            log.warn("清除用户缓存失败: email={}, error={}", email, e.getMessage());
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
            history.setCreatedAt(OffsetDateTime.now());
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
            user.setUpdatedAt(OffsetDateTime.now());
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
            user.setUpdatedAt(OffsetDateTime.now());
            int rows = userMapper.updateById(user);
            
            if (rows > 0) {
                // 5. 记录密码历史
                savePasswordHistory(userId, newPasswordHash);
                log.info("密码修改成功: userId={}", userId);
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
