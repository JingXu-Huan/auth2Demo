package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 密码历史实体
 * 对应 auth_db.password_history 表
 */
@Data
@Accessors(chain = true)
@TableName("password_history")
public class PasswordHistory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 历史密码哈希 */
    private String passwordHash;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
