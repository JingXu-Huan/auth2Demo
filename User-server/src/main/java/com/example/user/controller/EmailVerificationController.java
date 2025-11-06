package com.example.user.controller;

import com.example.common.dto.ApiResponse;
import com.example.user.service.EmailVerificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 邮箱验证控制器
 */
@Slf4j
@RestController
@Api(tags = "用户管理", description = "用户相关接口")
@RequestMapping("/api/users")
public class EmailVerificationController {
    
    /*
     * @auther Junjie 
     * @version 1.0.0
     * @date 2025-11-06
     * 自动装配用户邮箱验证服务
     */
    @Autowired
    private EmailVerificationService emailVerificationService;
    
    /**
     * @author Junjie
     * @version 2.0.0
     * @date 2025-11-06
     * 重新发送验证邮件（通过邮箱地址）
     * 不需要登录，直接提供邮箱地址即可
     */
    @ApiOperation(value = "重新发送验证邮件（通过邮箱地址）", notes = "当用户注册后未收到验证邮件或邮件过期时，可通过此接口重新发送")
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse> resendVerificationEmail(
        @ApiParam(value = "email", required = true, example = "user@example.com") 
        @RequestParam String email) {
        
        try {
            Map<String, Object> result = emailVerificationService.resendVerificationEmailByEmail(email);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(
                    "验证邮件已重新发送到您的邮箱", 
                    result.get("email")
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error((String) result.get("message")));
            }
        } catch (Exception e) {
            log.error("重发验证邮件失败: {}", email, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("发送失败，请稍后重试"));
        }
    }
    
    /**
     * @author Junjie
     * @version 2.0.0
     * @date 2025-11-06
     * 获取邮箱验证状态
     */
    @ApiOperation(value = "获取邮箱验证状态", notes = "获取当前用户的邮箱验证状态")
    @GetMapping("/verification-status")
    public ResponseEntity<ApiResponse> getVerificationStatus(
        @ApiParam(value = "request:请求数据(1:email)",required = true) 
        @RequestBody HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("未授权，请先登录"));
        }
        try {
            String token = authHeader.substring(7);
            Map<String, Object> status = emailVerificationService.getVerificationStatus(token);
            return ResponseEntity.ok(ApiResponse.success("获取成功", status));
        } catch (Exception e) {
            log.error("获取验证状态失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取失败"));
        }
    }
}
