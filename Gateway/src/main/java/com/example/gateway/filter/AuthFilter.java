package com.example.gateway.filter;

import brave.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.util.AntPathMatcher;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-09
 * Gateway 认证过滤器
 * 对所有请求进行认证，包括授权相关端点、公开的认证接口、公开的用户服务接口和安全验证接口。
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {
    
    @Autowired(required = false)
    private Tracer tracer;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 添加链路追踪信息到请求头
        ServerHttpRequest request = exchange.getRequest();
        if (tracer != null && tracer.currentSpan() != null) {
            String traceId = tracer.currentSpan().context().traceIdString();
            String spanId = tracer.currentSpan().context().spanIdString();
            
            request = request.mutate()
                .header("X-B3-TraceId", traceId)
                .header("X-B3-SpanId", spanId)
                .build();
            
            exchange = exchange.mutate().request(request).build();
        }
        
        String path = exchange.getRequest().getURI().getPath();
        AntPathMatcher matcher = new AntPathMatcher();

        // 放行授权相关端点
        if (matcher.match("/oauth/**", path) || matcher.match("/login.html", path)) {
            return chain.filter(exchange);
        }
        
        // 放行公开的认证接口（邮箱检查、用户名检查等）
        if (matcher.match("/api/auth/**", path)) {
            return chain.filter(exchange);
        }
        
        // 放行公开的用户服务接口（注册、邮箱验证、邮箱检查）
        if (matcher.match("/api/users/register", path) || 
            matcher.match("/api/users/confirm", path) ||
            matcher.match("/api/users/exists/email/**", path)) {
            return chain.filter(exchange);
        }
        
        // 放行安全验证接口（验证码发送和验证）
        if (matcher.match("/api/security/**", path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 令牌验证逻辑由资源服务器处理，网关只做基本检查
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -100;
    }
}