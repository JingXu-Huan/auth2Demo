package org.example.mpushservice.feign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置
 * 添加内部服务标识头
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 添加内部服务标识头，让 GatewayOnlyFilter 允许访问
            requestTemplate.header("X-Internal-Service", "IM-push-server");
        };
    }
}
