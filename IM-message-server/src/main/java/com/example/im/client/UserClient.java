package com.example.im.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 用户服务 Feign Client
 */
@FeignClient(name = "user-server")  // 使用Nacos服务发现
public interface UserClient {
    
    /**
     * 根据用户ID获取用户信息
     */
    @GetMapping("/api/v1/users/{userId}")
    Map<String, Object> getUserById(@PathVariable("userId") Long userId);
}
