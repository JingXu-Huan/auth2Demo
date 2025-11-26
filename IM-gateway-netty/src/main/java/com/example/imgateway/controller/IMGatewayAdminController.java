package com.example.imgateway.controller;

import com.example.imgateway.session.SessionManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * IM-Gateway-Netty 管理接口
 *
 * 提供健康检查和在线连接统计等基础管理能力。
 */
@Slf4j
@RestController
@RequestMapping("/gateway")
public class IMGatewayAdminController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public HealthResponse health() {
        HealthResponse resp = new HealthResponse();
        resp.setService("IM-Gateway-Netty");
        resp.setStatus("UP");
        resp.setTimestamp(Instant.now().toString());

        // Redis健康检查
        HealthResponse.Component redis = new HealthResponse.Component();
        redis.setName("redis");
        try {
            if (redisTemplate != null) {
                // 简单读操作作为健康检查
                redisTemplate.hasKey("im:health:probe");
                redis.setStatus("UP");
                redis.setMessage("Redis connection OK");
            } else {
                redis.setStatus("UNKNOWN");
                redis.setMessage("RedisTemplate not configured");
            }
        } catch (Exception e) {
            log.warn("Redis健康检查失败", e);
            redis.setStatus("DOWN");
            redis.setMessage("Redis connection failed: " + e.getMessage());
        }
        resp.setRedis(redis);

        return resp;
    }

    /**
     * 统计当前节点在线连接数
     */
    @GetMapping("/stats")
    public StatsResponse stats() {
        StatsResponse resp = new StatsResponse();
        resp.setOnlineConnections(sessionManager.getOnlineCount());
        resp.setTimestamp(Instant.now().toString());
        return resp;
    }

    @Data
    public static class HealthResponse {
        private String service;
        private String status;
        private String timestamp;
        private Component redis;

        @Data
        public static class Component {
            private String name;
            private String status;
            private String message;
        }
    }

    @Data
    public static class StatsResponse {
        private int onlineConnections;
        private String timestamp;
    }
}
