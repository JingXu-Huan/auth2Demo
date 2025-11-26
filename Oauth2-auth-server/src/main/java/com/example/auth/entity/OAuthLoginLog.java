package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * OAuth登录日志实体
 * 对应 auth_db.oauth_login_logs 表
 */
@Data
@Accessors(chain = true)
@TableName("oauth_login_logs")
public class OAuthLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** OAuth提供商 */
    private String provider;

    /** 提供商用户ID */
    private String providerUserId;

    /** 操作类型: authorize, callback, bind, unbind, login */
    private String action;

    /** 是否成功 */
    private Boolean success;

    /** 错误码 */
    private String errorCode;

    /** 错误消息 */
    private String errorMessage;

    /** IP地址 */
    private String ipAddress;

    /** 用户代理 */
    private String userAgent;

    /** OAuth state参数 */
    private String stateToken;

    /** OAuth授权码 */
    private String code;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
