package com.example.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * CSRF 保护配置
 * 
 * @author Security Team
 * @version 1.0.0
 */
@Configuration
public class CsrfConfig {
    
    /**
     * 配置 CSRF Token 存储方式
     * 使用 Cookie 存储，便于前后端分离架构使用
     * 
     * @return CsrfTokenRepository
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // 设置 Cookie 名称
        repository.setCookieName("XSRF-TOKEN");
        // 设置请求头名称
        repository.setHeaderName("X-XSRF-TOKEN");
        // 设置参数名称
        repository.setParameterName("_csrf");
        // Cookie 路径
        repository.setCookiePath("/");
        return repository;
    }
}
