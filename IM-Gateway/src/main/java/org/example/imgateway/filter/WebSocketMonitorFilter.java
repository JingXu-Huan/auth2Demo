package org.example.imgateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-13
 * WebSocket连接监控过滤器
 * 记录所有WebSocket连接信息和统计数据
 */
@Slf4j
@Component
public class WebSocketMonitorFilter implements GlobalFilter, Ordered {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.startsWith("/ws")) {
            logWebSocketConnection(exchange);
        }

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    if (path.startsWith("/ws")) {
                        log.debug("[WebSocket] 连接处理完成: {}", path);
                    }
                })
                .doOnError(throwable -> {
                    if (path.startsWith("/ws")) {
                        log.error("[WebSocket] 连接处理失败: {}, 错误: {}", path, throwable.getMessage());
                    }
                });
    }

    private void logWebSocketConnection(ServerWebExchange exchange) {
        try {
            String ip = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
            
            String path = exchange.getRequest().getPath().value();
            String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
            String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
            String timestamp = LocalDateTime.now().format(FORMATTER);
            
            // 提取用户ID（如果存在）
            String userId = extractUserIdFromPath(path);
            
            log.info("[WebSocket连接] 时间: {}, IP: {}, 用户: {}, 路径: {}", 
                    timestamp, ip, userId != null ? userId : "未知", path);
            
            if (userAgent != null) {
                log.debug("[WebSocket连接] User-Agent: {}", userAgent);
            }
            
            if (origin != null) {
                log.debug("[WebSocket连接] Origin: {}", origin);
            }
            
        } catch (Exception e) {
            log.warn("[WebSocket连接] 记录连接信息时发生错误: {}", e.getMessage());
        }
    }

    /**
     * 从路径中提取用户ID
     * 路径格式: /ws/{userId}
     */
    private String extractUserIdFromPath(String path) {
        if (path != null && path.startsWith("/ws/")) {
            String[] parts = path.split("/");
            if (parts.length >= 3) {
                return parts[2];
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -1;  // 优先级最高，确保最先执行
    }
}