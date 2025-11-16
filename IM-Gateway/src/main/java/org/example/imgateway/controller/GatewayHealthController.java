package org.example.imgateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-13
 * 网关健康检查控制器
 */
@RestController
@RequestMapping("/gateway")
@Slf4j
public class GatewayHealthController {

    @Autowired(required = false)
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    /**
     * 网关健康检查
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "IM-Gateway");
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return checkRedisConnection()
                .map(redisStatus -> {
                    response.put("redis", redisStatus);
                    return response;
                })
                .onErrorReturn(response);
    }

    /**
     * 网关统计信息
     */
    @GetMapping("/stats")
    public Mono<Map<String, Object>> stats() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "IM-Gateway");
        response.put("uptime", getUptime());
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return Mono.just(response);
    }

    /**
     * 检查Redis连接状态
     */
    private Mono<Map<String, Object>> checkRedisConnection() {
        Map<String, Object> redisStatus = new HashMap<>();
        
        if (reactiveStringRedisTemplate == null) {
            redisStatus.put("status", "DISABLED");
            redisStatus.put("message", "Redis template not available");
            return Mono.just(redisStatus);
        }
        
        return reactiveStringRedisTemplate
                .opsForValue()
                .set("gateway:health:check", "OK")
                .timeout(Duration.ofSeconds(2))
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> successStatus = new HashMap<>();
                    successStatus.put("status", "UP");
                    successStatus.put("message", "Redis connection successful");
                    return successStatus;
                }))
                .onErrorResume(throwable -> {
                    Map<String, Object> errorStatus = new HashMap<>();
                    errorStatus.put("status", "DOWN");
                    errorStatus.put("message", "Redis connection failed: " + throwable.getMessage());
                    return Mono.just(errorStatus);
                });
    }

    /**
     * 获取运行时间（简化版本）
     */
    private String getUptime() {
        long uptimeMs = System.currentTimeMillis() - startTime;
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        return String.format("%d小时%d分钟%d秒", 
                hours, minutes % 60, seconds % 60);
    }
    
    private static final long startTime = System.currentTimeMillis();
}
