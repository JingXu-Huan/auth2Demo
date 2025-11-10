package com.example.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-10
 * Gateway Sentinel 配置类
 * 配置网关流控规则和限流响应
 */
@Configuration
@Slf4j
public class GatewaySentinelConfig {
    
    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;
    
    public GatewaySentinelConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                 ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }
    
    /**
     * 配置 Sentinel 异常处理器
     * 注意：SentinelGatewayFilter 由 Spring Cloud Alibaba 自动配置，不需要手动创建
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }
    
    /**
     * 初始化配置
     * 注意：流控规则由 Nacos 动态管理，不在代码中硬编码
     */
    @PostConstruct
    public void init() {
        initBlockHandler();
        log.info("Gateway Sentinel 配置初始化完成（流控规则由 Nacos 动态管理）");
    }
    
    /**
     * 自定义限流响应
     */
    private void initBlockHandler() {
        BlockRequestHandler blockRequestHandler = (exchange, t) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 429);
            result.put("message", "请求过于频繁，请稍后再试");
            result.put("timestamp", System.currentTimeMillis());
            
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(result));
        };
        
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
        log.info("Gateway 限流响应处理器配置完成");
    }
}
