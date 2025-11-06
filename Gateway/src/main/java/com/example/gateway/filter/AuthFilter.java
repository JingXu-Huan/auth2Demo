package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.util.AntPathMatcher;

@Component
public class AuthFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
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