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
    fallback = com.example.auth.fallback.UserServiceClientFallback.class
)

public interface UserServiceClient {
    
    /**
     * 根据邮箱获取用户详情（用于登录认证）
     */
    @GetMapping("/api/users/details/email/{email}")
    UserDetailsDTO getUserDetailsByEmail(@PathVariable("email") String email);
    
    /**
     * 根据用户名获取用户详情
     */
    @GetMapping("/api/users/details/username/{username}")
    UserDetailsDTO getUserDetailsByUsername(@PathVariable("username") String username);
    
    /**
     * 检查邮箱是否存在
     */
    @GetMapping("/api/users/check-email")
    Result<Boolean> checkEmailExists(@RequestParam("email") String email);
    
    /**
     * 检查用户名是否存在
     */
    @GetMapping("/api/users/check-username")
    Result<Boolean> checkUsernameExists(@RequestParam("username") String username);
    
    /**
     * 根据用户ID获取用户信息
     */
    @GetMapping("/api/users/{userId}")
    Result<UserVO> getUserById(@PathVariable("userId") Long userId);
}
