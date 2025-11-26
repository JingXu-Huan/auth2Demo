package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户处罚实体
 * 对应 auth_db.user_punishments 表
 */
@Data
@Accessors(chain = true)
@TableName("user_punishments")
public class UserPunishment {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    /** 处罚类型: warning, mute, ban, permanent_ban */
    private String punishmentType;
    
    /** 处罚原因 */
    private String reason;
    
    /** 开始时间 */
    private LocalDateTime startAt;
    
    /** 结束时间 */
    private LocalDateTime endAt;
    
    /** 操作人ID */
    private Long operatorId;
    
    /** 是否生效 */
    private Boolean isActive;
    
    /** 解除时间 */
    private LocalDateTime liftedAt;
    
    /** 解除原因 */
    private String liftedReason;
    
    /** 解除人ID */
    private Long liftedBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 处罚类型常量
    public static final String TYPE_WARNING = "warning";
    public static final String TYPE_MUTE = "mute";
    public static final String TYPE_BAN = "ban";
    public static final String TYPE_PERMANENT_BAN = "permanent_ban";
}
