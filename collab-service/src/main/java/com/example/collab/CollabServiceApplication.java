package com.example.collab;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 文档协同服务启动类
 * 基于CRDT/Yjs实现实时协同编辑
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@MapperScan("com.example.collab.mapper")
public class CollabServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollabServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("文档协同服务启动成功！");
        System.out.println("端口: 8007");
        System.out.println("WebSocket: ws://localhost:8007/ws/doc");
        System.out.println("========================================");
    }
}
