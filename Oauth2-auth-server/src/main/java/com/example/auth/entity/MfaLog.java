package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 多因素认证日志实体
 * 对应 auth_db.mfa_logs 表
 */
@Data
@Accessors(chain = true)
@TableName("mfa_logs")
public class MfaLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** MFA类型: totp, sms, email */
    private String mfaType;
    
    /** 是否成功 */
    private Boolean success;
    
    /** 验证码 */
    private String code;
    
    /** IP地址 */
    private String ipAddress;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // MFA类型常量
    public static final String TYPE_TOTP = "totp";
    public static final String TYPE_SMS = "sms";
    public static final String TYPE_EMAIL = "email";
}
