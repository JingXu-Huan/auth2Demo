package com.example.user.controller;

import com.example.common.vo.Result;
import com.example.user.service.SecurityVerificationService;
import com.example.user.service.UserService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 安全验证控制器
 */
@RestController
@Api(tags = "安全验证", description = "安全验证相关接口")
@RequestMapping("/api/security")
public class SecurityController {
    
    /*
     * @auther Junjie 
     * @version 1.0.0
     * @date 2025-11-06
     * 自动装配 SecurityVerificationService
     */
    @Autowired
    private SecurityVerificationService securityService;
    
    /*
     * @auther Junjie 
     * @version 1.0.0
     * @date 2025-11-06
     * 自动装配 UserService
     */
    @Autowired
    private UserService userService;
    
    /*
     * @auther Junjie 
     * @version 1.0.0
     * @date 2025-11-06
     * 自动装配 PasswordEncoder
     */
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /*
     * @auther Junjie 
     * @version 1.0.0
     * @date 2025-11-06
     * 验证密码是否正确
     */
    @PostMapping("/validate-password")
    @ApiOperation(value = "验证密码是否正确")
    public Map<String, Object> validatePassword(
        @ApiParam(value = "request:请求数据(1:email,2:password)",required = true) 
        @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean valid = userService.validatePassword(email, password);
            result.put("valid", valid);
        } catch (Exception e) {
            result.put("valid", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /*
     * @auther Junjie 
     * @version 1.0.0
     * @date 2025-11-06
     * 检查是否需要安全验证
     */
    @GetMapping("/check-verification")
    @ApiOperation(value = "检查是否需要安全验证")
    public Map<String, Object> checkSecurityVerification(
        @ApiParam(value = "email:邮箱",required = true) 
        @RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean needs = securityService.needsSecurityVerification(email);
            result.put("needsVerification", needs);
            
            if (needs) {
                // 可以返回距离上次登录的天数
                result.put("daysSinceLastLogin", securityService.getDaysSinceLastLogin(email));
            }
        } catch (Exception e) {
            result.put("needsVerification", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /*
     * @auther Junjie 
     * @version 1.0.0
     * @date 2025-11-06
     * 发送安全验证码
     */
    @PostMapping("/send-code")
    @ApiOperation(value = "发送安全验证码")
    public Map<String, Object> sendSecurityCode(
        @ApiParam(value = "request:请求数据(1:email)",required = true) 
        @RequestBody Map<String, String> request) {
        String email = request.get("email");
        return securityService.sendSecurityCode(email);
    }
    
    /**
     * @auther Junjie 
     * @version 2.0.0
     * @date 2025-11-06
     * 验证安全验证码
     * 返回 Result<Boolean> 格式
     */
    @PostMapping("/verify-code")
    @ApiOperation(value = "验证安全验证码")
    public Result<Boolean> verifySecurityCode(
        @ApiParam(value = "request:请求数据(1:email,2:code)",required = true) 
        @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        
        boolean valid = securityService.verifySecurityCode(email, code);
        
        if (valid) {
            return Result.success("验证成功", true);
        } else {
            return Result.error(400, "验证码错误或已过期", false);
        }
    }
}
