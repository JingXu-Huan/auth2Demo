package com.example.auth.feign;

import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.vo.Result;
import com.example.domain.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * ====================================================================
 * User服务 Feign 客户端 (Declarative HTTP Client)
 * ====================================================================
 * 
 * 【Feign简介】
 * Feign是一个声明式的HTTP客户端，让服务间调用像本地方法调用一样简单：
 * - 只需定义接口，无需编写HTTP请求代码
 * - 自动集成服务发现（Nacos）
 * - 自动集成负载均衡（LoadBalancer）
 * - 支持降级处理（Fallback）
 * 
 * 【服务间调用流程】
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Oauth2-auth-server                                         │
 * │       ↓                                                     │
 * │  UserServiceClient.getUserDetailsByEmail("test@test.com")  │
 * │       ↓                                                     │
 * │  Feign 拦截方法调用                                          │
 * │       ↓                                                     │
 * │  从Nacos获取 user-server 的实例列表                          │
 * │       ↓                                                     │
 * │  LoadBalancer 选择一个实例                                   │
 * │       ↓                                                     │
 * │  发送 HTTP GET 请求                                          │
 * │       ↓                                                     │
 * │  User-server 处理请求并返回                                  │
 * │       ↓                                                     │
 * │  Feign 反序列化响应为 Result<UserDetailsDTO>                │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * 【核心注解说明】
 * @FeignClient(name = "user-server")
 *   - name: 目标服务在Nacos中注册的名称
 *   - fallback: 服务不可用时的降级处理类
 *   - url: 硬编码URL（已移除，使用服务发现）
 * 
 * 【降级处理 Fallback】
 * 当user-server不可用时，调用会自动转到UserServiceClientFallback
 * 返回默认值或错误信息，保证系统不会级联崩溃
 * 
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * @see UserServiceClientFallback 降级处理实现类
 */
@FeignClient(
    name = "user-server",              // 目标服务名（Nacos注册名）
    // 已移除url参数，使用Nacos服务发现
    fallback = com.example.auth.fallback.UserServiceClientFallback.class  // 降级处理
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
