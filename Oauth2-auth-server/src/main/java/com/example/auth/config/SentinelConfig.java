package com.example.auth.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Sentinel 配置类
 * 规则由 Sentinel 控制台动态管理，不在代码中硬编码
 */
@Configuration
@Slf4j
public class SentinelConfig {
    
    /**
     * 注册 Sentinel 切面，用于支持 @SentinelResource 注解
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
    
    /**
     * 初始化提示
     */
    @PostConstruct
    public void init() {
        log.info("Sentinel 配置初始化完成（规则由 Sentinel 控制台动态管理）");
    }
}
