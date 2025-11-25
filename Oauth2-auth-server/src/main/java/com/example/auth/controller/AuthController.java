package com.example.auth.controller;

import com.example.auth.feign.UserServiceClient;
import com.example.domain.vo.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 认证相关接口控制器
 * 提供用户注册、登录前的检查、JWT 登录等功能
 */
@Slf4j
@Tag(name = "API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserServiceClient userServiceClient;

    /**
     * 检查邮箱是否已存在
     */
    @Operation(summary = "检查邮箱是否已存在")
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(
            @Parameter(description = "电子邮箱")
            @RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            Result<Boolean> result = userServiceClient.checkEmailExists(email);
            boolean exists = result.getData() != null && result.getData();
            response.put("exists", exists);
            response.put("email", email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("exists", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 检查用户名是否已存在（暂不使用）
     */
    @Operation(summary = "检查用户名是否已存在（暂不使用）")
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(
            @Parameter(description = "用户名")
            @RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Result<Boolean> result = userServiceClient.checkUsernameExists(username);
            boolean exists = result.getData() != null && result.getData();
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("exists", false);
            response.put("message", "检查用户名失败");
            return ResponseEntity.ok(response);
        }
    }
    
    // 注意：JWT 用户名密码登录已废弃，统一使用 OAuth2 登录（Gitee）
}
