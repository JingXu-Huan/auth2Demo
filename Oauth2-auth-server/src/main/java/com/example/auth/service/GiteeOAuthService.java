package com.example.auth.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.auth.entity.OAuthBinding;
import com.example.auth.mapper.OAuthBindingMapper;
import com.example.auth.mapper.UserMapper;
import com.example.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.auth.config.GiteeProperties;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gitee OAuth 登录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GiteeOAuthService {
    
    private final OAuthBindingMapper oauthBindingMapper;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final GiteeProperties giteeProperties;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("========== GiteeOAuthService 配置加载 ==========");
        log.info("clientId: {}", giteeProperties.getClientId());
        log.info("redirectUri: {}", giteeProperties.getRedirectUri());
        log.info("authorizeUrl: {}", giteeProperties.getAuthorizeUrl());
        log.info("================================================");
    }
    
    /**
     * 生成Gitee授权URL
     */
    public String getAuthorizeUrl() {
        String state = UUID.randomUUID().toString();
        // 可以将state存入Redis，用于后续验证
        
        return String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&scope=user_info&state=%s",
                giteeProperties.getAuthorizeUrl(),
                giteeProperties.getClientId(), 
                giteeProperties.getRedirectUri(), 
                state);
    }
    
    /**
     * 处理Gitee回调
     * @param code 授权码
     * @param state 状态码
     * @return JWT token
     */
    @Transactional
    public Map<String, Object> handleCallback(String code, String state) {
        try {
            // 1. 验证state（应该从Redis中验证）
            
            // 2. 使用code换取access_token
            String tokenUrl = giteeProperties.getAccessTokenUrl();
            Map<String, Object> tokenParams = new HashMap<>();
            tokenParams.put("grant_type", "authorization_code");
            tokenParams.put("code", code);
            tokenParams.put("client_id", giteeProperties.getClientId());
            tokenParams.put("client_secret", giteeProperties.getClientSecret());
            tokenParams.put("redirect_uri", giteeProperties.getRedirectUri());
            
            String tokenResponse = HttpUtil.post(tokenUrl, tokenParams);
            JSONObject tokenJson = JSONUtil.parseObj(tokenResponse);
            
            if (tokenJson.containsKey("error")) {
                log.error("获取Gitee access_token失败: {}", tokenJson.getStr("error_description"));
                throw new RuntimeException("OAuth认证失败");
            }
            
            String accessToken = tokenJson.getStr("access_token");
            
            // 3. 使用access_token获取用户信息
            String userInfoUrl = giteeProperties.getUserInfoUrl() + "?access_token=" + accessToken;
            String userInfoResponse = HttpUtil.get(userInfoUrl);
            JSONObject userInfo = JSONUtil.parseObj(userInfoResponse);
            
            String giteeUserId = userInfo.getStr("id");
            String giteeUsername = userInfo.getStr("login");
            String giteeEmail = userInfo.getStr("email");
            String giteeNickname = userInfo.getStr("name");
            String giteeAvatar = userInfo.getStr("avatar_url");
            
            // 4. 查询是否已绑定
            OAuthBinding binding = oauthBindingMapper.findByProviderAndUserId("gitee", giteeUserId);
            
            OAuthBinding.User user;
            if (binding != null) {
                // 已绑定，更新token和登录时间
                binding.setAccessToken(accessToken);
                binding.setLastLoginAt(OffsetDateTime.now());
                // providerData 暂不存储，避免PostgreSQL jsonb类型问题
                // binding.setProviderData(convertToStandardMap(userInfo));
                oauthBindingMapper.updateById(binding);
                
                // 获取用户信息
                user = userMapper.selectById(binding.getUserId());
                
                // 如果用户邮箱未验证且Gitee提供了邮箱，自动验证
                if (user != null && giteeEmail != null && !Boolean.TRUE.equals(user.getEmailVerified())) {
                    user.setEmailVerified(true);
                    userMapper.updateById(user);
                }
            } else {
                // 新用户或未绑定
                // 尝试通过邮箱查找用户
                if (giteeEmail != null) {
                    user = userMapper.selectOne(new QueryWrapper<OAuthBinding.User>().eq("email", giteeEmail));
                } else {
                    user = null;
                }
                
                if (user == null) {
                    // 创建新用户
                    user = new OAuthBinding.User();
                    user.setUsername("gitee_" + giteeUsername);
                    user.setEmail(giteeEmail != null ? giteeEmail : giteeUsername + "@gitee.local");
                    user.setNickname(giteeNickname);
                    user.setAvatar(giteeAvatar);
                    // 使用随机密码，因为用户通过OAuth登录
                    user.setPasswordHash(UUID.randomUUID().toString());
                    user.setEmailVerified(giteeEmail != null); // 如果Gitee提供了邮箱，认为已验证
                    user.setStatus(1); // 正常状态
                    user.setCreatedAt(OffsetDateTime.now());
                    userMapper.insert(user);
                } else {
                    // 已存在的用户，如果Gitee提供了邮箱，更新邮箱验证状态
                    if (giteeEmail != null && !Boolean.TRUE.equals(user.getEmailVerified())) {
                        user.setEmailVerified(true);
                        userMapper.updateById(user);
                    }
                }
                
                // 创建绑定关系
                binding = new OAuthBinding();
                binding.setUserId(user.getId());
                binding.setProvider("gitee");
                binding.setProviderUserId(giteeUserId);
                binding.setProviderUsername(giteeUsername);
                binding.setProviderEmail(giteeEmail);
                binding.setProviderNickname(giteeNickname);
                binding.setProviderAvatarUrl(giteeAvatar);
                binding.setAccessToken(accessToken);
                // providerData 暂不存储，避免PostgreSQL jsonb类型问题
                // binding.setProviderData(convertToStandardMap(userInfo));
                binding.setIsPrimary(false);
                binding.setBindStatus(1); // 正常
                binding.setBoundAt(OffsetDateTime.now());
                binding.setLastLoginAt(OffsetDateTime.now());
                oauthBindingMapper.insert(binding);
            }
            
            // 5. 生成JWT token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("username", user.getUsername());
            claims.put("email", user.getEmail());
            claims.put("email_verified", user.getEmailVerified());
            claims.put("loginType", "gitee");
            
            String token = jwtUtil.generateToken(claims);
            
            // 6. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("nickname", user.getNickname());
            userMap.put("avatar", user.getAvatar());
            result.put("user", userMap);
            
            result.put("provider", "gitee");
            result.put("isNewUser", binding.getBoundAt().equals(binding.getLastLoginAt()));
            
            return result;
            
        } catch (Exception e) {
            log.error("Gitee OAuth登录失败", e);
            throw new RuntimeException("OAuth登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 绑定Gitee账号到现有用户
     */
    @Transactional
    public void bindGiteeAccount(Long userId, String code) {
        // 与handleCallback类似的流程，但是绑定到现有用户
        // 首先检查是否已经绑定
        if (oauthBindingMapper.existsByUserIdAndProvider(userId, "gitee")) {
            throw new RuntimeException("该用户已绑定Gitee账号");
        }
        
        // 然后执行OAuth流程获取Gitee用户信息并创建绑定关系
        // ... 代码类似handleCallback的前半部分
    }
    
    /**
     * 解绑Gitee账号
     */
    @Transactional
    public void unbindGiteeAccount(Long userId) {
        OAuthBinding binding = oauthBindingMapper.selectOne(
                new QueryWrapper<OAuthBinding>()
                        .eq("user_id", userId)
                        .eq("provider", "gitee")
                        .eq("bind_status", 1)
        );
        
        if (binding == null) {
            throw new RuntimeException("未绑定Gitee账号");
        }
        
        binding.setBindStatus(2); // 已解绑
        binding.setUnboundAt(OffsetDateTime.now());
        oauthBindingMapper.updateById(binding);
    }
    
    /**
     * 将 Hutool JSONObject 转换为标准 Java Map
     * 过滤掉 JSONNull 值，避免 Jackson 序列化失败
     */
    private Map<String, Object> convertToStandardMap(JSONObject jsonObject) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            // 过滤 Hutool 的 JSONNull
            if (value != null && !(value instanceof cn.hutool.json.JSONNull)) {
                if (value instanceof JSONObject) {
                    result.put(key, convertToStandardMap((JSONObject) value));
                } else {
                    result.put(key, value);
                }
            }
        }
        return result;
    }
}
