package com.example.file.service;

import com.example.file.entity.UserStorageQuota;
import com.example.file.mapper.UserStorageQuotaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户存储配额服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStorageService {
    
    private final UserStorageQuotaMapper userStorageQuotaMapper;
    
    // 默认配额 10GB
    private static final long DEFAULT_MAX_STORAGE = 10L * 1024 * 1024 * 1024;
    // 默认单文件最大 500MB
    private static final long DEFAULT_MAX_FILE_SIZE = 500L * 1024 * 1024;
    
    /**
     * 获取用户存储配额
     */
    public UserStorageQuota getUserQuota(Long userId) {
        UserStorageQuota quota = userStorageQuotaMapper.selectById(userId);
        if (quota == null) {
            // 创建默认配额
            quota = createDefaultQuota(userId);
        }
        return quota;
    }
    
    /**
     * 创建默认配额
     */
    @Transactional
    public UserStorageQuota createDefaultQuota(Long userId) {
        UserStorageQuota quota = new UserStorageQuota()
                .setUserId(userId)
                .setMaxStorage(DEFAULT_MAX_STORAGE)
                .setMaxFileSize(DEFAULT_MAX_FILE_SIZE)
                .setUsedStorage(0L)
                .setFileCount(0)
                .setExtraStorage(0L)
                .setUpdatedAt(LocalDateTime.now());
        userStorageQuotaMapper.insert(quota);
        return quota;
    }
    
    /**
     * 检查是否可以上传文件
     */
    public boolean canUpload(Long userId, long fileSize) {
        UserStorageQuota quota = getUserQuota(userId);
        
        // 检查单文件大小限制
        if (fileSize > quota.getMaxFileSize()) {
            return false;
        }
        
        // 检查总存储空间
        long totalAvailable = quota.getTotalAvailable();
        return quota.getUsedStorage() + fileSize <= totalAvailable;
    }
    
    /**
     * 增加存储使用量
     */
    @Transactional
    public void increaseUsage(Long userId, long size) {
        UserStorageQuota quota = getUserQuota(userId);
        quota.setUsedStorage(quota.getUsedStorage() + size);
        quota.setUpdatedAt(LocalDateTime.now());
        userStorageQuotaMapper.updateById(quota);
        log.info("增加用户存储使用量: userId={}, size={}", userId, size);
    }
    
    /**
     * 减少存储使用量
     */
    @Transactional
    public void decreaseUsage(Long userId, long size) {
        UserStorageQuota quota = getUserQuota(userId);
        quota.setUsedStorage(Math.max(0, quota.getUsedStorage() - size));
        quota.setUpdatedAt(LocalDateTime.now());
        userStorageQuotaMapper.updateById(quota);
        log.info("减少用户存储使用量: userId={}, size={}", userId, size);
    }
    
    /**
     * 增加赠送空间
     */
    @Transactional
    public void addBonusStorage(Long userId, long bonusSize, LocalDateTime expiresAt) {
        UserStorageQuota quota = getUserQuota(userId);
        long current = quota.getExtraStorage() != null ? quota.getExtraStorage() : 0;
        quota.setExtraStorage(current + bonusSize);
        quota.setExpiresAt(expiresAt);
        quota.setUpdatedAt(LocalDateTime.now());
        userStorageQuotaMapper.updateById(quota);
        log.info("增加用户赠送空间: userId={}, bonusSize={}", userId, bonusSize);
    }
    
    /**
     * 获取存储使用百分比
     */
    public int getUsagePercent(Long userId) {
        UserStorageQuota quota = getUserQuota(userId);
        long total = quota.getTotalAvailable();
        if (total == 0) return 0;
        return (int) (quota.getUsedStorage() * 100 / total);
    }
}
