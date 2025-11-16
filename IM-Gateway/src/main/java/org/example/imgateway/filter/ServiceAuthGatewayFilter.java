package org.example.imgateway.filter;

import com.example.common.config.ServiceAuthConfig;
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
 * @date 2025-11-13
 * IM Gateway 服务间认证过滤器
 * 为转发到后端服务的请求添加服务认证 Token
 */
@Component
@Slf4j
public class ServiceAuthGatewayFilter implements GlobalFilter, Ordered {

    @Autowired
    private ServiceAuthConfig authConfig;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 需要添加服务认证的路径（内部接口）
    private static final List<String> INTERNAL_API_PATTERNS = Arrays.asList(
        "/internal/**",                 // 所有内部接口
        "/api/chat/internal/**",       // 内部聊天接口
        "/api/chat/admin/**",          // 管理接口
        "/api/chat/system/**"          // 系统接口
    );
    
    // 公开接口，不需要添加服务认证
    private static final List<String> PUBLIC_API_PATTERNS = Arrays.asList(
        "/ws/**",                      // WebSocket连接
        "/api/chat/send",              // 公开消息发送
        "/api/chat/health",            // 健康检查
        "/api/chat/online-users",      // 在线用户数
        "/api/chat/private/**",        // 私聊接口
        "/api/chat/group/**",          // 群聊接口
        "/actuator/**",                // 监控端点
        "/gateway/**",                 // 网关管理接口
        "/swagger/**",                 // API文档
        "/v2/api-docs/**",            // Swagger文档
        "/doc.html**",                 // 文档页面
        "/static/**",                  // 静态资源
        "/index.html",                 // 测试页面
        "/favicon.ico"                 // 图标
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // 检查是否是公开接口
        boolean isPublicApi = PUBLIC_API_PATTERNS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (isPublicApi) {
            log.debug("公开接口，跳过服务认证: {}", path);
            return chain.filter(exchange);
        }
        
        // 检查是否是内部接口
        boolean isInternalApi = INTERNAL_API_PATTERNS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
        
        if (isInternalApi) {
            String serviceToken = authConfig.generateServiceToken();
            
            if (serviceToken != null) {
                ServerHttpRequest modifiedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-Service-Auth", serviceToken)
                    .header("Authorization", "Bearer " + serviceToken)
                    .header("X-Gateway-Service", "IM-Gateway")
                    .header("X-Request-ID", java.util.UUID.randomUUID().toString())
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
        
        // 对于其他接口，添加网关标识头
        ServerHttpRequest modifiedRequest = exchange.getRequest()
            .mutate()
            .header("X-Gateway-Service", "IM-Gateway")
            .build();
        
        ServerWebExchange modifiedExchange = exchange.mutate()
            .request(modifiedRequest)
            .build();
        
        return chain.filter(modifiedExchange);
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
