package com.example.auth.service;

import com.example.auth.config.GiteeProperties;
import com.example.domain.model.GiteeUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import com.example.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Gitee OAuth 服务
 * 处理 Gitee 授权登录的业务逻辑
 */
@Service
public class GiteeOAuthService {
    
    @Autowired
    private GiteeProperties giteeProperties;
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 构建 Gitee 授权 URL
     * 用户点击登录时跳转到这个URL
     */
    public String getAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromHttpUrl(giteeProperties.getAuthorizeUrl())
                .queryParam("client_id", giteeProperties.getClientId())
                .queryParam("redirect_uri", giteeProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .toUriString();
    }
    
    /**
     * 通过授权码获�?access_token
     * Gitee 回调后，�?code 换取 access_token
     */
    public String getAccessToken(String code) {
        // 构建请求参数
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("client_id", giteeProperties.getClientId());
        params.put("client_secret", giteeProperties.getClientSecret());
        params.put("redirect_uri", giteeProperties.getRedirectUri());
        
        // 发�?POST 请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
                giteeProperties.getAccessTokenUrl(),
                request,
                Map.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        
        throw new RuntimeException("获取 access_token 失败");
    }
    
    /**
     * 通过 access_token 获取用户信息
     */
    public GiteeUser getUserInfo(String accessToken) {
        // 构建请求URL（Gitee API 需要在 URL 中传�?access_token�?
        String url = UriComponentsBuilder
                .fromHttpUrl(giteeProperties.getUserInfoUrl())
                .queryParam("access_token", accessToken)
                .toUriString();
        
        // 发�?GET 请求
        ResponseEntity<GiteeUser> response = restTemplate.getForEntity(
                url,
                GiteeUser.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        }
        
        throw new RuntimeException("获取用户信息失败");
    }
}
