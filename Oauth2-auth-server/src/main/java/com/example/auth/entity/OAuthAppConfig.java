package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * OAuth应用配置实体
 * 存储Gitee、GitHub等第三方OAuth应用的配置信息
 */
@Data
@Accessors(chain = true)
@TableName(value = "oauth_app_configs", autoResultMap = true)
public class OAuthAppConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 提供商标识：gitee, github, wechat
     */
    private String provider;
    
    /**
     * 显示名称：Gitee、GitHub、微信
     */
    private String providerName;
    
    /**
     * OAuth客户端ID
     */
    private String clientId;
    
    /**
     * OAuth客户端密钥（应加密存储）
     */
    private String clientSecret;
    
    /**
     * 授权URL
     */
    private String authorizeUrl;
    
    /**
     * 获取token URL
     */
    private String tokenUrl;
    
    /**
     * 获取用户信息URL
     */
    private String userInfoUrl;
    
    /**
     * 撤销授权URL（可选）
     */
    private String revokeUrl;
    
    /**
     * 回调地址
     */
    private String redirectUri;
    
    /**
     * 请求的权限范围
     */
    private String scope;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 额外的OAuth参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> extraParams;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
