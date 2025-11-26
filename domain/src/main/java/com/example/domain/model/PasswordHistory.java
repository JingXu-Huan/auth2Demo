package com.example.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-10
 * 密码历史实体类
 */
@Data
@TableName("password_history")
public class PasswordHistory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 密码哈希
     */
    private String passwordHash;
    
    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;
}
