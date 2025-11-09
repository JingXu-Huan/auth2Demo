package com.example.auth.config;

import feign.Feign;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Feign Sentinel 配置
 * 用于 Feign 客户端的 Sentinel 集成
 */
@Configuration
public class FeignSentinelConfig {

    @Bean
    @Scope("prototype")
    public Feign.Builder feignBuilder() {
        return Feign.builder();
    }
}
