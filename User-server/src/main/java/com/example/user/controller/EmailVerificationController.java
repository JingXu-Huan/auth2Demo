package com.example.user.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.domain.dto.ApiResponse;
import com.example.user.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 邮箱验证控制器
 * 提供邮箱验证码发送和验证功能
 */
@Slf4j
@Tag(name = "API")
@RestController
@RequestMapping("/api/v1/email")
public class EmailVerificationController {
    
    @Autowired
    private EmailVerificationService emailVerificationService;
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 发送邮箱验证码
     */
    @Operation(summary = "发送邮箱验证码")
    @SentinelResource(value = "sendVerificationCode", blockHandler = "handleBlock")
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Parameter(description = "邮箱地址")
            @RequestBody Map<String, String> request) {
        
        try {
            String email = request.get("email");
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(400, "邮箱不能为空"));
            }
            
            // 检查是否已发送验证码
            if (emailVerificationService.hasVerificationCode(email)) {
                long remainingTime = emailVerificationService.getCodeRemainingTime(email);
                return ResponseEntity.ok(ApiResponse.error(429, 
                    "验证码已发送，请在 " + remainingTime + " 秒后重试"));
            }
            
            boolean sent = emailVerificationService.sendVerificationCode(email);
            
            if (sent) {
                return ResponseEntity.ok(ApiResponse.success("验证码已发送"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(500, "发送失败"));
            }
            
        } catch (Exception e) {
            log.error("发送验证码失败", e);
            return ResponseEntity.ok(ApiResponse.error(500, "发送失败"));
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 验证邮箱验证码
     */
    @Operation(summary = "验证邮箱验证码")
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(
            @Parameter(description = "验证请求")
            @RequestBody Map<String, String> request) {
        
        try {
            String email = request.get("email");
            String code = request.get("code");
            
            if (email == null || code == null) {
                return ResponseEntity.ok(ApiResponse.error(400, "参数不完整"));
            }
            
            boolean valid = emailVerificationService.verifyCode(email, code);
            
            if (valid) {
                return ResponseEntity.ok(ApiResponse.success(Boolean.TRUE, "验证成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(400, "验证码错误或已过期"));
            }
            
        } catch (Exception e) {
            log.error("验证码验证失败", e);
            return ResponseEntity.ok(ApiResponse.error(500, "验证失败"));
        }
    }
    
    /**
     * 验证邮箱并激活用户
     */
    @Operation(summary = "验证邮箱并激活用户")
    @PostMapping("/verify-and-activate")
    public ResponseEntity<ApiResponse<Void>> verifyAndActivate(
            @Parameter(description = "验证请求")
            @RequestBody Map<String, String> request) {
        
        try {
            String email = request.get("email");
            String code = request.get("code");
            
            if (email == null || code == null) {
                return ResponseEntity.ok(ApiResponse.error(400, "参数不完整"));
            }
            
            boolean success = emailVerificationService.verifyEmailAndActivate(email, code);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("邮箱验证成功，账户已激活"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(400, "验证失败"));
            }
            
        } catch (Exception e) {
            log.error("邮箱验证失败", e);
            return ResponseEntity.ok(ApiResponse.error(500, "验证失败"));
        }
    }
    
    /**
     * Sentinel 限流处理
     */
    public ResponseEntity<ApiResponse<Void>> handleBlock(Map<String, String> request, 
                                                         HttpServletRequest httpRequest) {
        log.warn("请求被限流: {}", httpRequest.getRequestURI());
        return ResponseEntity.ok(ApiResponse.error(429, "请求过于频繁，请稍后重试"));
    }
}
