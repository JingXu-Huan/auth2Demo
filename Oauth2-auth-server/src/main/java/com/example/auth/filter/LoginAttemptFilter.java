package com.example.auth.filter;

import com.example.auth.service.LoginAttemptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 登录尝试过滤器
 * 在 OAuth2 登录前检查是否被锁定
 */
@Slf4j
@Component
@Order(1)
public class LoginAttemptFilter extends OncePerRequestFilter {
    
    @Autowired
    private LoginAttemptService loginAttemptService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        // 只拦截 OAuth2 token 请求
        if (!"/oauth/token".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取用户名（从请求参数中）
        String username = request.getParameter("username");
        String grantType = request.getParameter("grant_type");
        
        // 只对密码模式进行检查
        if (!"password".equals(grantType) || username == null || username.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 检查是否被锁定
        if (loginAttemptService.isBlocked(username)) {
            long remainingTime = loginAttemptService.getBlockRemainingTime(username);
            
            log.warn("登录被阻止 - 账户已锁定: username={}, remainingTime={}秒", 
                    username, remainingTime);
            
            // 返回锁定错误
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "account_locked");
            errorResponse.put("error_description", 
                    "账户已被锁定，请在 " + remainingTime + " 秒后重试");
            errorResponse.put("remaining_seconds", remainingTime);
            
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }
        
        // 未被锁定，继续处理
        filterChain.doFilter(request, response);
    }
}
