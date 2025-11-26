package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 密码重置实体
 * 对应 auth_db.password_resets 表
 */
@Data
@Accessors(chain = true)
@TableName("password_resets")
public class PasswordReset {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 重置令牌 */
    private String token;
    
    /** 邮箱 */
    private String email;
    
    /** 是否已使用 */
    private Boolean used;
    
    /** 过期时间 */
    private LocalDateTime expiresAt;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /** 使用时间 */
    private LocalDateTime usedAt;
    
    /**
     * 检查是否有效（未使用且未过期）
     */
    public boolean isValid() {
        return !Boolean.TRUE.equals(used) && 
               expiresAt != null && 
               expiresAt.isAfter(LocalDateTime.now());
    }
}
