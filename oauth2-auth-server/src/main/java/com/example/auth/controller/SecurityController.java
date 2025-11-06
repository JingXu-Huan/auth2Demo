package com.example.auth.controller;

import com.example.auth.feign.UserServiceClient;
import com.example.common.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 安全验证控制器
 * 处理长时间未登录的安全验证
 */
@Api(tags = "安全验证", description = "长时间未登录的安全验证接口")
@RestController
@RequestMapping("/api/auth")
public class SecurityController {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 检查是否需要安全验证
     * 在用户输入密码后调用此接口
     */
    @ApiOperation(value = "检查安全验证", notes = "检查用户是否需要安全验证，如果需要则发送验证码")
    @PostMapping("/check-security")
    public ResponseEntity<Map<String, Object>> checkSecurity(
            @ApiParam(value = "请求参数，包含 email 和 password", required = true)
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 验证密码是否正确
            Map<String, String> validateRequest = new HashMap<>();
            validateRequest.put("email", email);
            validateRequest.put("password", password);
            Map<String, Object> validateResult = userServiceClient.validatePassword(validateRequest);
            
            if (!(Boolean) validateResult.get("valid")) {
                response.put("success", false);
                response.put("error", "密码错误");
                return ResponseEntity.status(401).body(response);
            }
            
            // 2. 检查是否需要安全验证
            Map<String, Object> securityCheck = userServiceClient.checkSecurityVerification(email);
            boolean needsVerification = (Boolean) securityCheck.get("needsVerification");
            
            if (needsVerification) {
                // 3. 发送验证码
                Map<String, String> sendRequest = new HashMap<>();
                sendRequest.put("email", email);
                Map<String, Object> sendResult = userServiceClient.sendSecurityCode(sendRequest);
                
                response.put("success", true);
                response.put("needsSecurityVerification", true);
                response.put("message", "检测到您的账号长时间未登录，验证码已发送到您的邮箱");
                response.put("daysSinceLastLogin", securityCheck.get("daysSinceLastLogin"));
                return ResponseEntity.ok(response);
            }
            
            // 4. 不需要验证，返回可以直接登录的标志
            response.put("success", true);
            response.put("needsSecurityVerification", false);
            response.put("message", "验证通过");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "服务器错误：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 验证安全验证码
     */
    @ApiOperation(value = "验证安全验证码", notes = "验证用户输入的安全验证码是否正确")
    @PostMapping("/verify-security-code")
    public ResponseEntity<Map<String, Object>> verifySecurityCode(
            @ApiParam(value = "请求参数，包含 email 和 code", required = true)
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证验证码（适配新的 Result<Boolean> 格式）
            Map<String, String> verifyRequest = new HashMap<>();
            verifyRequest.put("email", email);
            verifyRequest.put("code", code);
            Result<Boolean> verifyResult = userServiceClient.verifySecurityCode(verifyRequest);
            
            if (verifyResult.getCode() != 200 || !Boolean.TRUE.equals(verifyResult.getData())) {
                response.put("success", false);
                response.put("error", verifyResult.getMessage() != null ? verifyResult.getMessage() : "验证码错误或已过期");
                return ResponseEntity.status(400).body(response);
            }
            
            // 验证成功
            response.put("success", true);
            response.put("message", "验证成功，可以登录");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "验证失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 重新发送安全验证码
     */
    @ApiOperation(value = "重发安全验证码", notes = "重新发送安全验证码到用户邮箱")
    @PostMapping("/resend-security-code")
    public ResponseEntity<Map<String, Object>> resendSecurityCode(
            @ApiParam(value = "请求参数，包含 email", required = true)
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, String> sendRequest = new HashMap<>();
            sendRequest.put("email", email);
            Map<String, Object> sendResult = userServiceClient.sendSecurityCode(sendRequest);
            
            response.put("success", true);
            response.put("message", "验证码已重新发送");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "发送失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
