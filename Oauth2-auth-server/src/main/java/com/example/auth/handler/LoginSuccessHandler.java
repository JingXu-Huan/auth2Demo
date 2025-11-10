package com.example.auth.handler;

import com.example.auth.feign.UserServiceClient;
import com.example.auth.service.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-10
 * 登录成功处理器
 * 在用户登录成功后更新最后登录时间并清除登录失败记录
 */
@Slf4j
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private LoginAttemptService loginAttemptService;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       Authentication authentication) 
            throws ServletException, IOException {
        
        try {
            // 获取用户邮箱（username 字段存储的是邮箱）
            String email = authentication.getName();
            String ip = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            
            // 清除登录失败记录
            loginAttemptService.loginSucceeded(email);
            
            // 更新最后登录时间
            userServiceClient.updateLastLoginTime(email);
            
            // 记录结构化日志（供 ELK 收集）
            log.info("LOGIN_SUCCESS|email={}|ip={}|userAgent={}|device={}", 
                email, ip, userAgent, parseDeviceType(userAgent));
            
        } catch (Exception e) {
            log.error("处理登录成功失败", e);
            // 不影响登录流程，继续执行
        }
        
        // 调用父类方法继续处理
        super.onAuthenticationSuccess(request, response, authentication);
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 解析设备类型
     */
    private String parseDeviceType(String userAgent) {
        if (userAgent == null) {
            return "UNKNOWN";
        }
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "MOBILE";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "TABLET";
        } else {
            return "DESKTOP";
        }
    }
}
