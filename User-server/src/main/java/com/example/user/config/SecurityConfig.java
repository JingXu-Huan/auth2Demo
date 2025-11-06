package com.example.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 安全配置
 */
@Configuration
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
}
