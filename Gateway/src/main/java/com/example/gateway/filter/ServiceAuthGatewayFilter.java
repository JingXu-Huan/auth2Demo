package com.example.gateway.filter;

import com.example.common.config.ServiceAuthConfig;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-09
 * Gateway 服务间认证过滤器
 * 为转发到后端服务的请求添加服务认证 Token
 */
@Component
@Slf4j
public class ServiceAuthGatewayFilter implements GlobalFilter, Ordered {

    @Setter
    private ServiceAuthConfig authConfig;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 需要添加服务认证的路径（内部接口）.
    private static final List<String> INTERNAL_API_PATTERNS = Arrays.asList(
        "/api/users/details/**",
        "/api/users/internal/**"
    );
    
    //公开接口,比如登录接口,注册接口,确认注册接口,检查用户是否存在接口等等.
    private static final List<String> PUBLIC_API_PATTERNS = Arrays.asList(
        "/oauth/**",
        "/login.html",
        "/api/auth/**",
        "/api/users/register",
        "/api/users/confirm",
        "/api/users/exists/**",
        "/api/users/check-email",
        "/api/users/check-username",
        "/api/security/**",
        "/actuator/**",
        "/swagger**",
        "/v2/api-docs**",
        "/doc.html**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        boolean isPublicApi = PUBLIC_API_PATTERNS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (isPublicApi) {
            log.debug("公开接口，跳过服务认证: {}", path);
            return chain.filter(exchange);
        }
        
        boolean isInternalApi = INTERNAL_API_PATTERNS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
        
        if (isInternalApi) {
            String serviceToken = authConfig.generateServiceToken();
            
            if (serviceToken != null) {
                ServerHttpRequest modifiedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-Service-Auth", serviceToken)
                    .build();
                
                ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();
                
                log.debug("添加服务认证 Token: path={}", path);
                return chain.filter(modifiedExchange);
            } else {
                log.error("生成服务认证 Token 失败: path={}", path);
            }
        }
        
        return chain.filter(exchange);
    }

    /**
     * 设置过滤器执行顺序
     * 在 AuthFilter 之后执行，确保用户认证已完成
     *
     * @return 执行顺序值
     */
    @Override
    public int getOrder() {
        return -99;
    }

}
