package com.example.user.feign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * 为 User-server 的 Feign 客户端添加内部服务标识头
 * 注意: 不要添加 @Configuration 注解，否则会被全局扫描导致 factoryBeanObjectType 错误
 */
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 让 GatewayOnlyFilter 识别为内部服务调用
            requestTemplate.header("X-Internal-Service", "User-server");
        };
    }
}
