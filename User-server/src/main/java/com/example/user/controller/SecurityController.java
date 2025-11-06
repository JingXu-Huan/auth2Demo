package com.example.user.controller;

import com.example.domain.vo.Result;
import com.example.user.service.SecurityVerificationService;
import com.example.user.service.UserService;
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
 * 安全验证控制器
 * 提供密码验证、安全码等功能
 */
@Slf4j
@Api(tags = "安全验证", description = "密码验证和安全码管理")
@RestController
@RequestMapping("/api/security")
public class SecurityController {
    
    @Autowired
    private SecurityVerificationService securityVerificationService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 验证用户密码
     */
    @ApiOperation(value = "验证用户密码", notes = "用于敏感操作前的二次验证")
    @PostMapping("/validate-password")
    public ResponseEntity<Result<Boolean>> validatePassword(
            @ApiParam(value = "验证请求", required = true)
            @RequestBody Map<String, Object> request) {
        
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String password = request.get("password").toString();
            
            boolean valid = securityVerificationService.validatePassword(userId, password);
            
            if (valid) {
                return ResponseEntity.ok(Result.success("密码验证成功", Boolean.TRUE));
            } else {
                return ResponseEntity.ok(Result.error(400, "密码错误"));
            }
            
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return ResponseEntity.ok(Result.error(500, "验证失败"));
        }
    }
    
    /**
     * 发送安全验证码
     */
    @ApiOperation(value = "发送安全验证码", notes = "用于敏感操作的验证码发送")
    @PostMapping("/send-code")
    public ResponseEntity<Result<Void>> sendSecurityCode(
            @ApiParam(value = "发送请求", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String email = request.get("email");
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "邮箱不能为空"));
            }
            
            boolean sent = securityVerificationService.sendSecurityCode(email);
            
            if (sent) {
                return ResponseEntity.ok(Result.success("验证码已发送"));
            } else {
                return ResponseEntity.ok(Result.error(500, "发送失败"));
            }
            
        } catch (Exception e) {
            log.error("发送安全验证码失败", e);
            return ResponseEntity.ok(Result.error(500, "发送失败"));
        }
    }
    
    /**
     * 验证安全码
     */
    @ApiOperation(value = "验证安全码", notes = "验证用户输入的安全验证码")
    @PostMapping("/verify-code")
    public ResponseEntity<Result<Boolean>> verifySecurityCode(
            @ApiParam(value = "验证请求", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String email = request.get("email");
            String code = request.get("code");
            
            if (email == null || code == null) {
                return ResponseEntity.ok(Result.error(400, "参数不完整"));
            }
            
            boolean valid = securityVerificationService.verifySecurityCode(email, code);
            
            if (valid) {
                return ResponseEntity.ok(Result.success("验证成功", Boolean.TRUE));
            } else {
                return ResponseEntity.ok(Result.error(400, "验证码错误或已过期"));
            }
            
        } catch (Exception e) {
            log.error("验证安全码失败", e);
            return ResponseEntity.ok(Result.error(500, "验证失败"));
        }
    }
}
