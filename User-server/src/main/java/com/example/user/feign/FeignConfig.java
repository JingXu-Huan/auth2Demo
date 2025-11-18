package com.example.user.feign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为 User-server 的 Feign 客户端添加内部服务标识头
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 让 GatewayOnlyFilter 识别为内部服务调用
            requestTemplate.header("X-Internal-Service", "User-server");
        };
    }
}
