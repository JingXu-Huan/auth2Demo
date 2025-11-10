package com.example.auth.handler;

import com.example.auth.service.LoginAttemptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
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
 * 登录失败处理器
 * 记录登录失败次数，超过限制后锁定账户
 */
@Slf4j
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    @Autowired
    private LoginAttemptService loginAttemptService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       AuthenticationException exception) 
            throws IOException, ServletException {
        
        // 获取用户名和IP地址
        String username = request.getParameter("username");
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        if (username != null && !username.isEmpty()) {
            // 记录登录失败
            loginAttemptService.loginFailed(username);
            
            // 获取剩余尝试次数
            int remainingAttempts = loginAttemptService.getRemainingAttempts(username);
            
            // 记录结构化日志（供 ELK 收集）
            log.warn("LOGIN_FAILURE|email={}|ip={}|userAgent={}|device={}|reason={}|remainingAttempts={}", 
                    username, ipAddress, userAgent, parseDeviceType(userAgent), 
                    exception.getMessage(), remainingAttempts);
            
            // 如果是 OAuth2 token 请求，返回 JSON
            if ("/oauth/token".equals(request.getRequestURI())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_grant");
                errorResponse.put("error_description", exception.getMessage());
                errorResponse.put("remaining_attempts", remainingAttempts);
                
                if (remainingAttempts == 0) {
                    errorResponse.put("error_description", 
                        "登录失败次数过多，账户已被锁定15分钟");
                } else if (remainingAttempts <= 2) {
                    errorResponse.put("warning", 
                        "您还有 " + remainingAttempts + " 次尝试机会");
                }
                
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        }
        
        // 调用父类方法继续处理（表单登录）
        super.onAuthenticationFailure(request, response, exception);
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
