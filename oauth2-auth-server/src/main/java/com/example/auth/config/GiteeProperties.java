package com.example.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
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
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    
    public String getAuthorizeUrl() {
        return authorizeUrl;
    }
    
    public void setAuthorizeUrl(String authorizeUrl) {
        this.authorizeUrl = authorizeUrl;
    }
    
    public String getAccessTokenUrl() {
        return accessTokenUrl;
    }
    
    public void setAccessTokenUrl(String accessTokenUrl) {
        this.accessTokenUrl = accessTokenUrl;
    }
    
    public String getUserInfoUrl() {
        return userInfoUrl;
    }
    
    public void setUserInfoUrl(String userInfoUrl) {
        this.userInfoUrl = userInfoUrl;
    }
}
