package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 第三方OAuth绑定实体
 * 支持Gitee、GitHub、微信等多种OAuth登录方式
 */
@Data
@Accessors(chain = true)
@TableName(value = "oauth_bindings", autoResultMap = true)
public class OAuthBinding {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * OAuth提供商：gitee, github, wechat, qq, google, microsoft
     */
    private String provider;
    
    /**
     * 第三方平台的用户ID
     */
    private String providerUserId;
    
    /**
     * 第三方平台的用户名
     */
    private String providerUsername;
    
    /**
     * OAuth访问令牌
     */
    private String accessToken;
    
    /**
     * OAuth刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌过期时间
     */
    private OffsetDateTime tokenExpiresAt;
    
    /**
     * 第三方用户邮箱
     */
    private String providerEmail;
    
    /**
     * 第三方用户昵称
     */
    private String providerNickname;
    
    /**
     * 第三方用户头像URL
     */
    private String providerAvatarUrl;
    
    /**
     * 第三方用户主页URL
     */
    private String providerProfileUrl;
    
    /**
     * 第三方完整用户信息（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> providerData;
    
    /**
     * 是否为主要登录方式
     */
    private Boolean isPrimary;
    
    /**
     * 绑定状态：1:正常, 2:已解绑, 3:禁用
     */
    private Integer bindStatus;
    
    /**
     * 绑定时间
     */
    private OffsetDateTime boundAt;
    
    /**
     * 最后登录时间
     */
    private OffsetDateTime lastLoginAt;
    
    /**
     * 解绑时间
     */
    private OffsetDateTime unboundAt;

    /**
     * 用户实体类
     */
    @Data
    @Accessors(chain = true)
    @TableName("users")
    public static class User {

        @TableId(type = IdType.AUTO)
        private Long id;

        /**
         * 用户名
         */
        private String username;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 手机号
         */
        private String phone;

        /**
         * 密码哈希
         */
        private String passwordHash;

        /**
         * 昵称（存储在 user_profiles 表中，此处仅用于传输）
         */
        @TableField(exist = false)
        private String nickname;

        /**
         * 头像URL（存储在 user_profiles 表中，此处仅用于传输）
         */
        @TableField(exist = false)
        private String avatar;

        /**
         * 用户状态：1:正常, 2:禁用, 3:锁定, 4:未激活
         */
        private Integer status;

        /**
         * 邮箱是否已验证
         */
        private Boolean emailVerified;

        /**
         * 手机是否已验证
         */
        private Boolean phoneVerified;

        /**
         * 是否启用MFA
         */
        private Boolean mfaEnabled;

        /**
         * MFA密钥
         */
        private String mfaSecret;

        /**
         * 最后登录时间
         */
        private OffsetDateTime lastLoginAt;

        /**
         * 最后登录IP
         */
        private String lastLoginIp;

        /**
         * 登录失败次数
         */
        private Integer failedLoginCount;

        /**
         * 锁定截止时间
         */
        private OffsetDateTime lockedUntil;

        /**
         * 创建时间
         */
        private OffsetDateTime createdAt;

        /**
         * 更新时间
         */
        private OffsetDateTime updatedAt;

        /**
         * 删除时间（软删除）
         */
        private OffsetDateTime deletedAt;
    }
}
