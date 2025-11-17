package com.example.user.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.domain.dto.ApiResponse;
import com.example.user.service.LoginSecurityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-10
 * 登录安全验证控制器
 * 处理长时间未登录的安全验证
 */
@Slf4j
@Api(tags = "登录安全验证", description = "长时间未登录的安全验证管理")
@RestController
@RequestMapping("/api/v1/security")
public class LoginSecurityController {
    
    @Autowired
    private LoginSecurityService loginSecurityService;
    
    /**
     * 检查是否需要安全验证
     * 当用户长时间未登录需要进行安全验证时，调用此接口检查是否需要发送验证码
     */
    @ApiOperation(value = "检查是否需要安全验证", notes = "检查用户是否长时间未登录，需要进行安全验证")
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSecurityVerification(
            @ApiParam(value = "用户邮箱", required = true)
            @RequestParam String email) {
        
        try {
            boolean needsVerification = loginSecurityService.needsSecurityVerification(email);
            long daysSinceLastLogin = loginSecurityService.getDaysSinceLastLogin(email);
            
            Map<String, Object> result = new HashMap<>();
            result.put("needsVerification", needsVerification);
            result.put("daysSinceLastLogin", daysSinceLastLogin);
            
            if (needsVerification) {
                result.put("message", "您已超过30天未登录，需要进行安全验证");
            } else {
                result.put("message", "无需安全验证");
            }
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("检查安全验证失败", e);
            return ResponseEntity.ok(ApiResponse.error(500, "检查失败"));
        }
    }
    
    /**
     * 发送安全验证码
     * 当用户长时间未登录需要进行安全验证时，调用此接口发送安全验证码
     */
    @ApiOperation(value = "发送安全验证码", notes = "向用户邮箱发送安全验证码")
    @SentinelResource(value = "sendSecurityCode", blockHandler = "handleBlock")
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendSecurityCode(
            @ApiParam(value = "邮箱地址", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String email = request.get("email");
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(400, "邮箱不能为空"));
            }
            
            boolean sent = loginSecurityService.sendSecurityVerificationCode(email);
            
            if (sent) {
                return ResponseEntity.ok(ApiResponse.success("安全验证码已发送"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(500, "发送失败"));
            }
            
        } catch (Exception e) {
            log.error("发送安全验证码失败", e);
            return ResponseEntity.ok(ApiResponse.error(500, "发送失败"));
        }
    }
    
    /**
     * 验证安全验证码
     * 当用户输入安全验证码后，调用此接口验证验证码是否正确
     */
    @ApiOperation(value = "验证安全验证码", notes = "验证用户输入的安全验证码")
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Boolean>> verifySecurityCode(
            @ApiParam(value = "验证请求", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String email = request.get("email");
            String code = request.get("code");
            
            if (email == null || code == null) {
                return ResponseEntity.ok(ApiResponse.error(400, "参数不完整"));
            }
            
            boolean valid = loginSecurityService.verifySecurityCode(email, code);
            
            if (valid) {
                return ResponseEntity.ok(ApiResponse.success(Boolean.TRUE, "验证成功，15分钟内可以登录"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(400, "验证码错误或已过期"));
            }
            
        } catch (Exception e) {
            log.error("验证安全验证码失败", e);
            return ResponseEntity.ok(ApiResponse.error(500, "验证失败"));
        }
    }
    
    /**
     * Sentinel 限流处理
     */
    public ResponseEntity<ApiResponse<Void>> handleBlock(Map<String, String> request) {
        log.warn("请求被限流");
        return ResponseEntity.ok(ApiResponse.error(429, "请求过于频繁，请稍后重试"));
    }
}
