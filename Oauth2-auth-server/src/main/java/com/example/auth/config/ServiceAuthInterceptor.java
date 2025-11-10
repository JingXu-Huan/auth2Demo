package com.example.auth.config;

import brave.Tracer;
import com.example.common.config.ServiceAuthConfig;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Feign 请求拦截器
 * 为每个 Feign 请求添加服务认证 Token
 */
@Component
@Slf4j
public class ServiceAuthInterceptor implements RequestInterceptor {
    
    @Autowired
    private ServiceAuthConfig authConfig;
    
    @Autowired(required = false)
    private Tracer tracer;
    
    @Override
    public void apply(RequestTemplate template) {
        // 传递链路追踪信息
        if (tracer != null && tracer.currentSpan() != null) {
            String traceId = tracer.currentSpan().context().traceIdString();
            String spanId = tracer.currentSpan().context().spanIdString();
            template.header("X-B3-TraceId", traceId);
            template.header("X-B3-SpanId", spanId);
            log.debug("添加链路追踪信息: traceId={}, spanId={}", traceId, spanId);
        }
        try {
            // 为每个 Feign 请求添加服务认证 Token
            String token = authConfig.generateServiceToken();
            if (token != null) {
                template.header("X-Service-Auth", token);
                log.debug("添加服务认证 Token 到请求: {}", template.url());
            } else {
                log.warn("生成服务认证 Token 失败");
            }
        } catch (Exception e) {
            log.error("添加服务认证 Token 失败", e);
        }
    }
}
