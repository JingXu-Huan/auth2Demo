package com.example.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.auth.entity.OAuthBinding;
import com.example.auth.mapper.OAuthBindingMapper;
import com.example.auth.feign.UserServiceClient;
import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.model.GiteeUser;
import com.example.domain.vo.Result;
import com.example.domain.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一OAuth服务
 * 处理所有第三方OAuth登录（Gitee、GitHub、微信等）
 */
@Slf4j
@Service
public class OAuthService {
    
    @Autowired
    private OAuthBindingMapper oauthBindingMapper;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private LoginLogService loginLogService;
    
    /**
     * 处理OAuth登录回调
     * @param provider OAuth提供商
     * @param code 授权码
     * @param providerUser 第三方用户信息
     * @return 用户信息和绑定信息
     */
    @Transactional
    public Map<String, Object> handleOAuthCallback(String provider, String code, Object providerUser) {
        try {
            // 根据不同的provider处理用户信息
            String providerUserId = null;
            String username = null;
            String email = null;
            String avatar = null;
            Map<String, Object> providerData = new HashMap<>();
            
            if ("gitee".equals(provider) && providerUser instanceof GiteeUser) {
                GiteeUser giteeUser = (GiteeUser) providerUser;
                providerUserId = String.valueOf(giteeUser.getId());
                username = giteeUser.getLogin();
                email = giteeUser.getEmail();
                avatar = giteeUser.getAvatarUrl();
                providerData.put("name", giteeUser.getName());
                providerData.put("htmlUrl", giteeUser.getHtmlUrl());
                providerData.put("bio", giteeUser.getBio());
            }
            // 可以添加更多provider的处理逻辑
            
            // 查找是否存在绑定
            OAuthBinding binding = oauthBindingMapper.findByProviderAndUserId(provider, providerUserId);
            
            Long userId = null;
            boolean isNewUser = false;
            boolean isNewBinding = false;
            
            if (binding != null && binding.getBindStatus() == 1) {
                // 已存在绑定，更新信息
                userId = binding.getUserId();
                updateOAuthBinding(binding, email, username, avatar, providerData);
                log.info("OAuth用户登录: provider={}, userId={}, providerUserId={}", 
                    provider, userId, providerUserId);
            } else {
                // 不存在绑定或已解绑
                // 尝试通过邮箱查找用户
                UserDetailsDTO existingUser = null;
                if (email != null && !email.isEmpty()) {
                    Result<UserDetailsDTO> userResult = userServiceClient.getUserDetailsByEmail(email);
                    if (userResult != null && userResult.getData() != null) {
                        existingUser = userResult.getData();
                    }
                }
                
                if (existingUser != null) {
                    // 用户已存在，创建绑定关系
                    userId = existingUser.getUserId();
                    isNewBinding = true;
                    createOAuthBinding(userId, provider, providerUserId, username, email, avatar, providerData);
                    log.info("为现有用户创建OAuth绑定: userId={}, provider={}", userId, provider);
                } else {
                    // 创建新用户
                    Result<UserDetailsDTO> createResult = userServiceClient.createOrUpdateOAuthUser(
                        provider, providerUserId, username, email, avatar
                    );
                    
                    if (createResult == null || createResult.getData() == null) {
                        throw new RuntimeException("创建OAuth用户失败");
                    }
                    
                    UserDetailsDTO newUser = createResult.getData();
                    userId = newUser.getUserId();
                    isNewUser = true;
                    
                    // 创建绑定关系
                    createOAuthBinding(userId, provider, providerUserId, username, email, avatar, providerData);
                    log.info("创建新OAuth用户: userId={}, provider={}", userId, provider);
                }
            }
            
            // 获取用户详情
            Result<UserDetailsDTO> userResult = userServiceClient.getUserDetailsByEmail(email != null ? email : username);
            if (userResult == null || userResult.getData() == null) {
                throw new RuntimeException("获取用户信息失败");
            }
            
            UserDetailsDTO userDetails = userResult.getData();
            
            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("username", userDetails.getUsername());
            result.put("email", userDetails.getEmail());
            result.put("nickname", userDetails.getDisplayName());
            result.put("avatar", userDetails.getAvatarUrl());
            result.put("provider", provider);
            result.put("isNewUser", isNewUser);
            result.put("isNewBinding", isNewBinding);
            result.put("emailVerified", userDetails.getEmailVerified());
            
            return result;
            
        } catch (Exception e) {
            log.error("处理OAuth回调失败: provider={}", provider, e);
            throw new RuntimeException("OAuth登录失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建OAuth绑定
     */
    private void createOAuthBinding(Long userId, String provider, String providerUserId,
                                   String username, String email, String avatar,
                                   Map<String, Object> providerData) {
        OAuthBinding binding = new OAuthBinding();
        binding.setUserId(userId);
        binding.setProvider(provider);
        binding.setProviderUserId(providerUserId);
        binding.setProviderUsername(username);
        binding.setProviderEmail(email);
        binding.setProviderNickname(username);
        binding.setProviderAvatarUrl(avatar);
        binding.setProviderData(providerData);
        binding.setIsPrimary(false);
        binding.setBindStatus(1);
        binding.setBoundAt(LocalDateTime.now());
        binding.setLastLoginAt(LocalDateTime.now());
        
        oauthBindingMapper.insert(binding);
    }
    
    /**
     * 更新OAuth绑定信息
     */
    private void updateOAuthBinding(OAuthBinding binding, String email, String username,
                                   String avatar, Map<String, Object> providerData) {
        binding.setProviderEmail(email);
        binding.setProviderUsername(username);
        binding.setProviderNickname(username);
        binding.setProviderAvatarUrl(avatar);
        binding.setProviderData(providerData);
        binding.setLastLoginAt(LocalDateTime.now());
        
        oauthBindingMapper.updateById(binding);
    }
    
    /**
     * 获取用户的OAuth绑定列表
     */
    public List<OAuthBinding> getUserOAuthBindings(Long userId) {
        return oauthBindingMapper.selectList(
            new QueryWrapper<OAuthBinding>()
                .eq("user_id", userId)
                .eq("bind_status", 1)
        );
    }
    
    /**
     * 解绑OAuth账号
     */
    @Transactional
    public boolean unbindOAuth(Long userId, String provider) {
        try {
            // 检查是否有其他登录方式
            List<OAuthBinding> bindings = getUserOAuthBindings(userId);
            if (bindings.size() <= 1) {
                // 检查是否有密码登录
                Result<UserVO> userResult = userServiceClient.getUserById(userId);
                if (userResult == null || userResult.getData() == null) {
                    log.warn("用户不存在或没有其他登录方式，不能解绑: userId={}, provider={}", userId, provider);
                    return false;
                }
            }
            
            // 执行解绑
            OAuthBinding binding = oauthBindingMapper.selectOne(
                new QueryWrapper<OAuthBinding>()
                    .eq("user_id", userId)
                    .eq("provider", provider)
                    .eq("bind_status", 1)
            );
            
            if (binding == null) {
                log.warn("绑定关系不存在: userId={}, provider={}", userId, provider);
                return false;
            }
            
            binding.setBindStatus(2); // 已解绑
            binding.setUnboundAt(LocalDateTime.now());
            oauthBindingMapper.updateById(binding);
            
            log.info("OAuth解绑成功: userId={}, provider={}", userId, provider);
            return true;
            
        } catch (Exception e) {
            log.error("OAuth解绑失败: userId={}, provider={}", userId, provider, e);
            return false;
        }
    }
    
    /**
     * 设置主要登录方式
     */
    @Transactional
    public boolean setPrimaryOAuth(Long userId, String provider) {
        try {
            // 先将所有绑定设置为非主要
            OAuthBinding updateBinding = new OAuthBinding();
            updateBinding.setIsPrimary(false);
            oauthBindingMapper.update(updateBinding,
                new QueryWrapper<OAuthBinding>()
                    .eq("user_id", userId)
            );
            
            // 设置指定的为主要
            OAuthBinding binding = oauthBindingMapper.selectOne(
                new QueryWrapper<OAuthBinding>()
                    .eq("user_id", userId)
                    .eq("provider", provider)
                    .eq("bind_status", 1)
            );
            
            if (binding == null) {
                return false;
            }
            
            binding.setIsPrimary(true);
            oauthBindingMapper.updateById(binding);
            
            log.info("设置主要OAuth登录方式: userId={}, provider={}", userId, provider);
            return true;
            
        } catch (Exception e) {
            log.error("设置主要OAuth失败: userId={}, provider={}", userId, provider, e);
            return false;
        }
    }
}
