package com.example.push.interceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 认证拦截器
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String jwtSecret;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // 优先从URL参数获取userId（开发模式）
            String userIdParam = servletRequest.getServletRequest().getParameter("userId");
            if (StringUtils.hasText(userIdParam)) {
                attributes.put("userId", Long.parseLong(userIdParam));
                log.info("WebSocket 握手成功(开发模式): userId={}", userIdParam);
                return true;
            }
        }
        
        // 从URL参数获取token
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket 握手失败: 缺少token或userId");
            return false;
        }

        try {
            // 验证token
            Claims claims = verifyToken(token);
            
            // 获取用户ID
            Object userId = claims.get("userId");
            if (userId == null) {
                userId = claims.getSubject();
            }
            
            if (userId == null) {
                log.warn("WebSocket 握手失败: token中缺少userId");
                return false;
            }

            // 设置用户信息到attributes
            attributes.put("userId", userId);
            attributes.put("token", token);
            
            // 获取设备ID（如果有）
            Object deviceId = claims.get("deviceId");
            if (deviceId != null) {
                attributes.put("deviceId", deviceId);
            }

            log.info("WebSocket 握手成功: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error("WebSocket 握手失败: token验证错误 - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后处理
    }

    /**
     * 从请求中获取token
     */
    private String getTokenFromRequest(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // 从URL参数获取
            String token = servletRequest.getServletRequest().getParameter("token");
            if (StringUtils.hasText(token)) {
                return token;
            }
            
            // 从Header获取
            String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        return null;
    }

    /**
     * 验证JWT Token
     */
    private Claims verifyToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
