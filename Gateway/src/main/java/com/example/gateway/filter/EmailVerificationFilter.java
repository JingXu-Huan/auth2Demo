package com.example.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Base64Utils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 邮箱验证过滤器
 * 检查用户是否已验证邮箱，未验证的用户只能访问特定接口
 */
@Component
public class EmailVerificationFilter implements GlobalFilter, Ordered {
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * JSON 对象转换器
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 路径匹配器
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 不需要验证邮箱的路径（白名单）
     */
    private static final String[] WHITELIST_PATHS = {
        "/oauth/**",           // OAuth2 认证相关
        "/api/users/register", // 用户注册
        "/api/users/confirm",  // 邮箱验证
        "/api/users/exists/email/**", // 邮箱存在性检查
        "/api/users/resend-verification", // 重发验证邮件
        "/api/security/**",    // 安全验证接口
        "/api/auth/**",        // 认证相关
        "/login.html",         // 登录页面
        "/static/**",          // 静态资源
        "/favicon.ico",        // 图标
        "/error"               // 错误页面
    };
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 过滤器实现
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // 检查是否是白名单路径
        for (String whitePath : WHITELIST_PATHS) {
            if (PATH_MATCHER.match(whitePath, path)) {
                return chain.filter(exchange);
            }
        }
        
        // 从 Authorization header 中提取 token
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 没有 token，可能需要登录
            return chain.filter(exchange);
        }
        
        String token = authHeader.substring(7);
        
        // 解析 JWT token（这里简化处理，实际应该使用 JWT 库）
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return chain.filter(exchange);
            }
            
            // 解码 payload
            String payload = new String(Base64Utils.decodeFromUrlSafeString(parts[1]), StandardCharsets.UTF_8);
            Map tokenInfo = OBJECT_MAPPER.readValue(payload, Map.class);
            
            // 检查邮箱验证状态
            Boolean emailVerified = (Boolean) tokenInfo.get("email_verified");
            
            if (emailVerified == null || !emailVerified) {
                // 邮箱未验证，返回错误响应
                return sendErrorResponse(exchange, 
                    HttpStatus.FORBIDDEN, 
                    "EMAIL_NOT_VERIFIED", 
                    "请先验证您的邮箱才能访问系统资源");
            }
            
            // 邮箱已验证，继续处理
            return chain.filter(exchange);
            
        } catch (Exception e) {
            // Token 解析失败，让后续的认证过滤器处理
            return chain.filter(exchange);
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 发送错误响应
     */
    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, HttpStatus status, 
                                         String errorCode, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", status.value());
        errorBody.put("error", errorCode);
        errorBody.put("message", message);
        errorBody.put("timestamp", System.currentTimeMillis());
        
        try {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(errorBody);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 获取过滤器优先级
     */
    @Override
    public int getOrder() {
        // 在认证过滤器之后运行
        return -50;
    }
}
