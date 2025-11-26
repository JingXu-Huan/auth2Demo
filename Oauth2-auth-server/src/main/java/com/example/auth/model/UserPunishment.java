package com.example.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户惩罚记录实体
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPunishment {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 被惩罚的用户ID
     */
    private Long userId;
    
    /**
     * 惩罚类型：BAN-封禁，MUTE-禁言，KICK-踢下线，WARNING-警告
     */
    private String type;
    
    /**
     * 持续时长（秒），NULL表示永久
     */
    private Integer duration;
    
    /**
     * 惩罚原因
     */
    private String reason;
    
    /**
     * 执行操作的管理员ID
     */
    private Long operatorId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 是否已撤销
     */
    private Boolean revoked;
    
    /**
     * 撤销时间
     */
    private LocalDateTime revokedAt;
    
    /**
     * 撤销原因
     */
    private String revokeReason;
    
    /**
     * 惩罚类型枚举
     */
    public enum PunishmentType {
        BAN,      // 封禁
        MUTE,     // 禁言
        KICK,     // 踢下线
        WARNING   // 警告
    }
}
