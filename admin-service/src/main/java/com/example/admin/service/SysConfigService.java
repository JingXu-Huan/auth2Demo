package com.example.admin.service;

import com.example.admin.entity.SysConfig;
import com.example.admin.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigService {
    
    private final SysConfigMapper sysConfigMapper;
    private final StringRedisTemplate redisTemplate;
    
    private static final String CONFIG_CACHE_PREFIX = "sys:config:";
    
    /**
     * 获取配置值
     */
    public String getConfig(String key) {
        // 先查缓存
        String cacheKey = CONFIG_CACHE_PREFIX + key;
        String value = redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            return value;
        }
        
        // 查数据库
        SysConfig config = sysConfigMapper.selectById(key);
        if (config != null) {
            redisTemplate.opsForValue().set(cacheKey, config.getConfigValue(), 1, TimeUnit.HOURS);
            return config.getConfigValue();
        }
        return null;
    }
    
    /**
     * 设置配置值
     */
    public void setConfig(String key, String value, String description, Long adminId) {
        SysConfig config = sysConfigMapper.selectById(key);
        if (config == null) {
            config = new SysConfig()
                    .setConfigKey(key)
                    .setConfigValue(value)
                    .setDescription(description)
                    .setUpdatedBy(adminId)
                    .setUpdatedAt(LocalDateTime.now());
            sysConfigMapper.insert(config);
        } else {
            config.setConfigValue(value)
                    .setUpdatedBy(adminId)
                    .setUpdatedAt(LocalDateTime.now());
            sysConfigMapper.updateById(config);
        }
        
        // 更新缓存
        String cacheKey = CONFIG_CACHE_PREFIX + key;
        redisTemplate.opsForValue().set(cacheKey, value, 1, TimeUnit.HOURS);
        
        log.info("更新系统配置: {} = {}, admin={}", key, value, adminId);
    }
    
    /**
     * 获取所有配置
     */
    public List<SysConfig> getAllConfigs() {
        return sysConfigMapper.selectList(null);
    }
}
