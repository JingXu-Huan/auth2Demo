package com.example.auth.config;

import com.example.auth.feign.UserServiceClient;
import com.example.domain.dto.UserDetailsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 自定义 Token 增强器
 * 在 Token 中添加额外的用户信息
 */
@Component
public class CustomTokenEnhancer implements TokenEnhancer {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        Map<String, Object> additionalInfo = new HashMap<>();
        
        // 获取用户名（实际是邮箱）
        String email = authentication.getName();
        
        try {
            // 通过 Feign 调用 User-server 获取用户详细信息
            UserDetailsDTO userDetails = userServiceClient.getUserDetailsByEmail(email);
            
            if (userDetails != null) {
                // 添加用户ID
                additionalInfo.put("user_id", userDetails.getUserId());
                // 添加用户名
                additionalInfo.put("username", userDetails.getUsername());
                // 添加邮箱
                additionalInfo.put("email", userDetails.getEmail());
                // 添加登录方式
                additionalInfo.put("provider", userDetails.getProvider());
            }
        } catch (Exception e) {
            // 如果获取用户信息失败，只添加基本信息
            additionalInfo.put("email", email);
        }
        
        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
        return accessToken;
    }
}
