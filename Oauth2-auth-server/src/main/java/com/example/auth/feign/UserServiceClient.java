package com.example.auth.feign;

import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.vo.Result;
import com.example.domain.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;




/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * User 服务 Feign 客户端
 * 用于调用 User-server 的接口
 */
@FeignClient(
    name = "user-server",
    url = "http://localhost:8001",  // User-server 端口 8001
    fallback = com.example.auth.fallback.UserServiceClientFallback.class
)

public interface UserServiceClient {
    
    /**
     * 根据邮箱获取用户详情（用于登录认证）
     */
    @GetMapping("/api/v1/users/details/email/{email}")
    Result<UserDetailsDTO> getUserDetailsByEmail(@PathVariable("email") String email);
    
    /**
     * 根据用户名获取用户详情
     */
    @GetMapping("/api/v1/users/details/username/{username}")
    Result<UserDetailsDTO> getUserDetailsByUsername(@PathVariable("username") String username);
    
    /**
     * 检查邮箱是否存在
     */
    @GetMapping("/api/v1/users/check-email")
    Result<Boolean> checkEmailExists(@RequestParam("email") String email);
    
    /**
     * 检查用户名是否存在
     */
    @GetMapping("/api/v1/users/check-username")
    Result<Boolean> checkUsernameExists(@RequestParam("username") String username);
    
    /**
     * 根据用户ID获取用户信息
     */
    @GetMapping("/api/v1/users/{userId}")
    Result<UserVO> getUserById(@PathVariable("userId") Long userId);
    
    /**
     * 更新用户最后登录时间
     */
    @PostMapping("/api/v1/users/update-login-time")
    void updateLastLoginTime(@RequestParam("email") String email);
    
    /**
     * 创建或更新OAuth用户（Gitee、GitHub等第三方登录）
     */
    @PostMapping("/api/v1/users/oauth/create-or-update")
    Result<UserDetailsDTO> createOrUpdateOAuthUser(
        @RequestParam("provider") String provider,
        @RequestParam("providerUserId") String providerUserId,
        @RequestParam("username") String username,
        @RequestParam("email") String email,
        @RequestParam("avatarUrl") String avatarUrl
    );
}
