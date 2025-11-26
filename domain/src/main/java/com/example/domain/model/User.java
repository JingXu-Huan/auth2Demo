package com.example.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 用户主表实体 - 严格对应 auth_db.users 表结构
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
@TableName("users")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String email;
    
    private String phone;
    
    @JsonIgnore
    private String passwordHash;
    
    /**
     * 用户状态: 1=正常, 2=禁用, 3=锁定, 4=未激活
     */
    private Short status;
    
    private Boolean emailVerified;
    
    private Boolean phoneVerified;
    
    // ========== 安全相关 ==========
    
    private Boolean mfaEnabled;
    
    @JsonIgnore
    private String mfaSecret;
    
    private OffsetDateTime lastLoginAt;
    
    private String lastLoginIp;  // INET 类型映射为 String
    
    private Integer failedLoginCount;
    
    private OffsetDateTime lockedUntil;
    
    // ========== 时间戳 ==========
    
    private OffsetDateTime createdAt;
    
    private OffsetDateTime updatedAt;
    
    // 软删除字段：NULL=未删除，有值=删除时间
    // 注意：不使用 @TableLogic，因为 MyBatis-Plus 不支持 TIMESTAMPTZ 类型的逻辑删除
    // 查询时需手动添加 deleted_at IS NULL 条件
    private OffsetDateTime deletedAt;
}
