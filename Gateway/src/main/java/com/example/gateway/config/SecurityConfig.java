package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf().disable()
            // CORS 由 CorsGlobalConfiguration 统一处理
            .authorizeExchange(exchanges -> exchanges
                // 允许所有 OAuth 相关路径
                .pathMatchers("/oauth/**").permitAll()
                .pathMatchers("/login.html").permitAll()
                // 公开的认证接口（邮箱检查、用户名检查等）
                .pathMatchers("/api/auth/**").permitAll()
                // 公开的用户服务接口（注册、邮箱验证）
                .pathMatchers("/api/users/register").permitAll()
                .pathMatchers("/api/users/confirm").permitAll()
                // 其他所有路径都允许（由后端服务自己控制权限）
                .anyExchange().permitAll()
            );
        
        return http.build();
    }
}
