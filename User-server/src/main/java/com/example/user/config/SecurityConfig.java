package com.example.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 安全配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /** 
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 提供密码加密器 Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 配置Spring Security过滤链
     * 允许所有API请求通过，由ServiceAuthFilter进行服务间认证
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // 禁用CSRF，因为这是API服务
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // 允许所有请求，由ServiceAuthFilter控制访问
            );
        
        return http.build();
    }
}
