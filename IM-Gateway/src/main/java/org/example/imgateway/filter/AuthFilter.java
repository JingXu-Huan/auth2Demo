package org.example.imgateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-13
 * IM Gateway 认证过滤器
 * 对所有请求进行认证，包括WebSocket连接、聊天API和管理接口
 */
@Component
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethodValue())) {
            return chain.filter(exchange);
        }
        AntPathMatcher matcher = new AntPathMatcher();

        log.debug("Processing auth for path: {}", path);

        // 放行WebSocket连接（WebSocket认证在连接时处理）
        if (matcher.match("/ws/**", path)) {
            log.debug("Allowing WebSocket connection: {}", path);
            return chain.filter(exchange);
        }
        
        // 放行健康检查和监控端点
        if (matcher.match("/actuator/**", path) || 
            matcher.match("/gateway/**", path)) {
            log.debug("Allowing health/monitoring endpoint: {}", path);
            return chain.filter(exchange);
        }
        
        // 放行静态资源和文档
        if (matcher.match("/static/**", path) || 
            matcher.match("/index.html", path) ||
            matcher.match("/favicon.ico", path) ||
            matcher.match("/swagger/**", path) ||
            matcher.match("/v2/api-docs/**", path) ||
            matcher.match("/doc.html", path)) {
            log.debug("Allowing static resource: {}", path);
            return chain.filter(exchange);
        }
        
        // 放行公开的聊天API（基础功能）
        if (matcher.match("/api/chat/health", path) ||
            matcher.match("/api/chat/online-users", path)) {
            log.debug("Allowing public chat API: {}", path);
            return chain.filter(exchange);
        }

        // 检查用户认证Token
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        log.debug("User authentication passed for path: {}", path);
        // 令牌验证逻辑由后端服务处理，网关只做基本检查
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -100;  // 最高优先级，在其他过滤器之前执行
    }
}
