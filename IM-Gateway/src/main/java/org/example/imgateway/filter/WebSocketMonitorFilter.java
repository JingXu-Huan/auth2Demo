package org.example.imgateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

/**
*
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-09
 * WebSocket连接监控过滤器
 * 记录所有WebSocket连接信息
 */
@Slf4j
@Component
public class WebSocketMonitorFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.startsWith("/ws")) {
            String ip = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
            log.info("[WebSocket连接] IP: {}, Path: {}", ip, path);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;  // 优先级最高
    }
}