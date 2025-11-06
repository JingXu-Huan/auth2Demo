package com.example.auth.config;

import com.example.auth.feign.UserServiceClient;
import com.example.common.dto.UserDetailsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义 Token 增强器
 * 在 Token 中添加用户邮箱验证状态等额外信息
 */
@Component
public class CustomTokenEnhancer implements TokenEnhancer {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        Map<String, Object> additionalInfo = new HashMap<>();
        
        String username = authentication.getName();
        
        // 获取用户详细信息
        UserDetailsDTO userDetails = null;
        if (username.contains("@")) {
            userDetails = userServiceClient.getUserDetailsByEmail(username);
        } else {
            userDetails = userServiceClient.getUserDetails(username);
        }
        
        if (userDetails != null) {
            // 添加额外信息到 Token
            additionalInfo.put("user_id", userDetails.getUserId());
            additionalInfo.put("email", userDetails.getEmail());
            additionalInfo.put("email_verified", userDetails.getEmailVerified());
            additionalInfo.put("username", userDetails.getUsername());
            additionalInfo.put("display_name", userDetails.getDisplayName());
        }
        
        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
        return accessToken;
    }
}
