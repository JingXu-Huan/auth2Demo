package com.example.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户存储配额实体
 * 对应 file_db.user_storage_quotas 表
 */
@Data
@Accessors(chain = true)
@TableName("user_storage_quotas")
public class UserStorageQuota {
    
    @TableId(type = IdType.INPUT)
    private Long userId;
    
    /** 最大存储空间(字节) */
    private Long maxStorage;
    
    /** 最大单文件大小(字节) */
    private Long maxFileSize;
    
    /** 已使用存储 */
    private Long usedStorage;
    
    /** 文件数量 */
    private Integer fileCount;
    
    /** 额外赠送空间 */
    private Long extraStorage;
    
    /** 配额过期时间 */
    private LocalDateTime expiresAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    // 默认配额常量
    public static final long DEFAULT_MAX_STORAGE = 10737418240L; // 10GB
    public static final long DEFAULT_MAX_FILE_SIZE = 524288000L; // 500MB
    
    /**
     * 获取总可用空间
     */
    public long getTotalAvailable() {
        long extra = extraStorage != null ? extraStorage : 0;
        return (maxStorage != null ? maxStorage : DEFAULT_MAX_STORAGE) + extra;
    }
    
    /**
     * 获取剩余空间
     */
    public long getRemainingSpace() {
        return getTotalAvailable() - (usedStorage != null ? usedStorage : 0);
    }
}
