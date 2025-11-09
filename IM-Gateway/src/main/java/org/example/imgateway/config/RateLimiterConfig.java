package org.example.imgateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-09
 * IM网关限流配置
 * 用于防止恶意连接和DDoS攻击
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 基于IP的限流Key解析器（默认）
     * 用于防止单个IP过多连接
     @Bean
     */
    @Primary  // 设置为主要的KeyResolver
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
            return Mono.just(ip);
        };
    }

    /**
     * 基于用户的限流Key解析器
     * 可用于防止单个用户过多请求
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // 从请求头或参数中获取用户标识
            String username = exchange.getRequest()
                    .getQueryParams()
                    .getFirst("username");
            return Mono.just(username != null ? username : "anonymous");
        };
    }
}