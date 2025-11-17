package com.example.auth.handler;

import com.example.auth.feign.UserServiceClient;
import com.example.auth.service.LoginAttemptService;
import com.example.common.util.JwtUtil;
import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.vo.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       Authentication authentication) 
            throws ServletException, IOException {
        
        // 获取用户邮箱（username 字段存储的是邮箱）
        String email = authentication.getName();
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        try {
            // 清除登录失败记录
            loginAttemptService.loginSucceeded(email);
            
            // 更新最后登录时间
            userServiceClient.updateLastLoginTime(email);
            
            // 获取用户详情
            com.example.domain.vo.Result<UserDetailsDTO> userResponse = 
                userServiceClient.getUserDetailsByEmail(email);
            
            if (userResponse == null || userResponse.getData() == null) {
                throw new RuntimeException("无法获取用户信息");
            }
            
            UserDetailsDTO userDetails = userResponse.getData();
            
            // 生成 JWT token（包含邮箱验证状态）
            String token = jwtUtil.generateToken(
                userDetails.getUserId(),
                userDetails.getUsername(),
                userDetails.getEmailVerified()
            );
            
            // 构造返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", userDetails.getUserId());
            data.put("username", userDetails.getUsername());
            data.put("email", userDetails.getEmail());
            data.put("displayName", userDetails.getDisplayName());
            data.put("avatarUrl", userDetails.getAvatarUrl());
            
            // 返回 JSON 响应
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            
            Result<Map<String, Object>> result = Result.success("登录成功", data);
            response.getWriter().write(objectMapper.writeValueAsString(result));
            
            // 记录日志
            log.info("LOGIN_SUCCESS|email={}|ip={}|userAgent={}|device={}", 
                email, ip, userAgent, parseDeviceType(userAgent));
            
        } catch (Exception e) {
            log.error("登录成功处理失败", e);
            
            // 返回错误响应
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            Result<Object> errorResult = Result.error(500, "登录处理失败: " + e.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(errorResult));
        }
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
