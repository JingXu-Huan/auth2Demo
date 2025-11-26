package com.example.user.service;

import com.example.domain.model.UserProfile;
import com.example.user.mapper.UserProfileMapper;
import com.example.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户详情服务
 * 管理用户的详细个人信息
 */
@Slf4j
@Service
public class UserProfileService {
    
    @Autowired
    private UserProfileMapper userProfileMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 获取用户详情
     */
    @Cacheable(value = "userProfile", key = "#p0")
    public UserProfile getUserProfile(Long userId) {
        try {
            UserProfile profile = userProfileMapper.getByUserId(userId);
            
            if (profile == null) {
                // 如果不存在，创建默认的用户详情
                profile = createDefaultProfile(userId);
            }
            
            return profile;
        } catch (Exception e) {
            log.error("获取用户详情失败: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 创建默认的用户详情
     */
    @Transactional
    private UserProfile createDefaultProfile(Long userId) {
        try {
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            profile.setNickname("用户" + userId);
            profile.setGender(0); // 未知
            profile.setTimezone("Asia/Shanghai");
            profile.setLocale("zh-CN");
            profile.setUpdatedAt(OffsetDateTime.now());
            
            userProfileMapper.insert(profile);
            log.info("创建默认用户详情: userId={}", userId);
            
            return profile;
        } catch (Exception e) {
            log.error("创建默认用户详情失败: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 更新用户基本信息
     */
    @CacheEvict(value = "userProfile", key = "#p0")
    @Transactional
    public boolean updateBasicInfo(Long userId, String nickname, String realName,
                                  Integer gender, String birthday, String bio) {
        try {
            // 确保用户详情存在
            UserProfile profile = getUserProfile(userId);
            if (profile == null) {
                return false;
            }
            
            // 更新信息
            int rows = userProfileMapper.updateBasicInfo(
                userId, nickname, realName, gender, birthday, bio
            );
            
            if (rows > 0) {
                log.info("更新用户基本信息成功: userId={}", userId);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("更新用户基本信息失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 更新用户头像
     */
    @CacheEvict(value = "userProfile", key = "#p0")
    @Transactional
    public boolean updateAvatar(Long userId, String avatarUrl) {
        try {
            int rows = userProfileMapper.updateAvatar(userId, avatarUrl);
            
            if (rows > 0) {
                log.info("更新用户头像成功: userId={}, avatarUrl={}", userId, avatarUrl);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("更新用户头像失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 更新工作信息
     */
    @CacheEvict(value = "userProfile", key = "#p0")
    @Transactional
    public boolean updateWorkInfo(Long userId, String company, String department,
                                 String position, String employeeId) {
        try {
            int rows = userProfileMapper.updateWorkInfo(
                userId, company, department, position, employeeId
            );
            
            if (rows > 0) {
                log.info("更新用户工作信息成功: userId={}", userId);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("更新用户工作信息失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 更新地址信息
     */
    @CacheEvict(value = "userProfile", key = "#p0")
    @Transactional
    public boolean updateAddressInfo(Long userId, String country, String province,
                                    String city, String address) {
        try {
            UserProfile profile = userProfileMapper.getByUserId(userId);
            if (profile == null) {
                profile = createDefaultProfile(userId);
            }
            
            profile.setCountry(country);
            profile.setProvince(province);
            profile.setCity(city);
            profile.setAddress(address);
            profile.setUpdatedAt(OffsetDateTime.now());
            
            userProfileMapper.updateById(profile);
            
            log.info("更新用户地址信息成功: userId={}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("更新用户地址信息失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 更新偏好设置
     */
    @CacheEvict(value = "userProfile", key = "#p0")
    @Transactional
    public boolean updatePreferences(Long userId, String timezone, String locale) {
        try {
            UserProfile profile = userProfileMapper.getByUserId(userId);
            if (profile == null) {
                profile = createDefaultProfile(userId);
            }
            
            profile.setTimezone(timezone);
            profile.setLocale(locale);
            profile.setUpdatedAt(OffsetDateTime.now());
            
            userProfileMapper.updateById(profile);
            
            log.info("更新用户偏好设置成功: userId={}, timezone={}, locale={}", 
                userId, timezone, locale);
            return true;
            
        } catch (Exception e) {
            log.error("更新用户偏好设置失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 更新扩展字段
     */
    @CacheEvict(value = "userProfile", key = "#p0")
    @Transactional
    public boolean updateExtra(Long userId, Map<String, Object> extra) {
        try {
            UserProfile profile = userProfileMapper.getByUserId(userId);
            if (profile == null) {
                profile = createDefaultProfile(userId);
            }
            
            // 合并扩展字段
            Map<String, Object> currentExtra = profile.getExtra();
            if (currentExtra == null) {
                currentExtra = new HashMap<>();
            }
            currentExtra.putAll(extra);
            
            profile.setExtra(currentExtra);
            profile.setUpdatedAt(OffsetDateTime.now());
            
            userProfileMapper.updateById(profile);
            
            log.info("更新用户扩展字段成功: userId={}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("更新用户扩展字段失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 验证用户是否完善了个人信息
     */
    public boolean isProfileComplete(Long userId) {
        try {
            UserProfile profile = getUserProfile(userId);
            
            if (profile == null) {
                return false;
            }
            
            // 检查必填字段是否完善
            return profile.getRealName() != null && !profile.getRealName().isEmpty() &&
                   profile.getGender() != null && profile.getGender() != 0;
                   
        } catch (Exception e) {
            log.error("检查用户资料完整性失败: userId={}", userId, e);
            return false;
        }
    }
}
