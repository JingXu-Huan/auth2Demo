package com.example.auth.controller;

import com.example.auth.feign.UserServiceClient;
import com.example.domain.vo.Result;
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
 * 提供密码验证、安全码发送等功能
 */
@Slf4j
@Api(tags = "安全验证", description = "密码验证和安全码管理")
@RestController
@RequestMapping("/api/security")
public class SecurityController {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    /**
     * 验证用户密码（用于敏感操作前的二次验证）
     * 注意：此接口已废弃，建议直接在 User-server 中实现
     */
    @ApiOperation(value = "验证用户密码", notes = "用于敏感操作前的二次验证")
    @PostMapping("/validate-password")
    public ResponseEntity<Result<Boolean>> validatePassword(
            @ApiParam(value = "验证请求", required = true)
            @RequestBody Map<String, String> request) {
        
        log.warn("validatePassword 接口已废弃，请使用 User-server 的接口");
        return ResponseEntity.ok(Result.error(501, "接口已废弃"));
    }
    
    /**
     * 发送安全验证码（用于敏感操作）
     * 注意：此接口已废弃，建议直接在 User-server 中实现
     */
    @ApiOperation(value = "发送安全验证码", notes = "用于敏感操作的验证码发送")
    @PostMapping("/send-code")
    public ResponseEntity<Result<Void>> sendSecurityCode(
            @ApiParam(value = "发送请求", required = true)
            @RequestBody Map<String, String> request) {
        
        log.warn("sendSecurityCode 接口已废弃，请使用 User-server 的接口");
        return ResponseEntity.ok(Result.error(501, "接口已废弃"));
    }
}
