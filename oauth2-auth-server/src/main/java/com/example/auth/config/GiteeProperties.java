package com.example.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Gitee OAuth 配置属性
 */
@Component
@ConfigurationProperties(prefix = "gitee.oauth")
public class GiteeProperties {
    
    /**
     * 应用ID（Client ID）
     */
    private String clientId;
    
    /**
     * 应用密钥（Client Secret）
     */
    private String clientSecret;
    
    /**
     * 授权回调地址
     */
    private String redirectUri;
    
    /**
     * 授权页面URL
     */
    private String authorizeUrl;
    
    /**
     * 获取access_token的URL
     */
    private String accessTokenUrl;
    
    /**
     * 获取用户信息的URL
     */
    private String userInfoUrl;
    
    // Getter和Setter方法
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public String getClientSecret() {
        return clientSecret;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public String getRedirectUri() {
        return redirectUri;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public String getAuthorizeUrl() {
        return authorizeUrl;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public void setAuthorizeUrl(String authorizeUrl) {
        this.authorizeUrl = authorizeUrl;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public String getAccessTokenUrl() {
        return accessTokenUrl;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public void setAccessTokenUrl(String accessTokenUrl) {
        this.accessTokenUrl = accessTokenUrl;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public String getUserInfoUrl() {
        return userInfoUrl;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public void setUserInfoUrl(String userInfoUrl) {
        this.userInfoUrl = userInfoUrl;
    }
}
