package com.example.auth.controller;

import com.example.auth.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证相关接口
 */
@Api(tags = "认证管理", description = "用户认证相关接口")
@RestController
@RequestMapping("/api/auth")
// CORS 由网关统一处理
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 检查邮箱是否存在
     */
    @ApiOperation(value = "检查邮箱是否存在", notes = "用于注册时验证邮箱是否已被使用")
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(
            @ApiParam(value = "邮箱地址", required = true, example = "user@example.com")
            @RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean exists = userService.checkEmailExists(email);
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
     * 检查用户名是否存在
     */
    @ApiOperation(value = "检查用户名是否存在", notes = "用于注册时验证用户名是否已被使用")
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(
            @ApiParam(value = "用户名", required = true, example = "testuser")
            @RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean exists = userService.usernameExists(username);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("exists", false);
            response.put("message", "检查用户名失败");
            return ResponseEntity.ok(response);
        }
    }
}
