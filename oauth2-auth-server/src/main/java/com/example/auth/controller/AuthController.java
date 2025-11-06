package com.example.auth.controller;

import com.example.auth.feign.UserServiceClient;
import com.example.domain.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证相关接口控制器
 * 提供用户注册、登录前的检查等功能
 */
@Api(tags = "认证管理", description = "用户认证相关接口控制器")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserServiceClient userServiceClient;

    /**
     * 检查邮箱是否已存在
     */
    @ApiOperation(value = "检查邮箱是否已存在", notes = "用于注册时验证邮箱是否已被使用（返回true表示已存在）")
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(
            @ApiParam(value = "电子邮箱", required = true, example = "user@example.com")
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
    @ApiOperation(value = "检查用户名是否已存在（暂不使用）", notes = "用于注册时验证用户名是否已被其他账户使用（返回true表示已存在）")
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(
            @ApiParam(value = "用户名", required = true, example = "testuser")
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
}
