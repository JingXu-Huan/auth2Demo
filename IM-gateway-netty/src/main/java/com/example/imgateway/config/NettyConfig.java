package com.example.imgateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Netty配置
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "netty")
public class NettyConfig {
    
    /**
     * WebSocket监听端口
     */
    private int port = 9090;
    
    /**
     * Boss线程数（处理连接）
     */
    private int bossThreads = 1;
    
    /**
     * Worker线程数（处理IO）
     */
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    
    /**
     * 读空闲超时（秒）
     */
    private int readerIdleTime = 180;
    
    /**
     * 写空闲超时（秒）
     */
    private int writerIdleTime = 0;
    
    /**
     * 读写空闲超时（秒）
     */
    private int allIdleTime = 0;
    
    /**
     * SO_BACKLOG - 握手队列长度
     */
    private int backlog = 1024;
    
    /**
     * 最大帧长度（防止超大包攻击）
     */
    private int maxFrameLength = 65536;
}
