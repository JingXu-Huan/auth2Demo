package com.example.async;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 异步写服务启动类
 * 使用Java 21虚拟线程处理异步任务
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
@MapperScan("com.example.async.mapper")
public class AsyncServerApplication {

    public static void main(String[] args) {
        // 启用虚拟线程
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(AsyncServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("异步写服务启动成功！(Java 21 Virtual Threads)");
        System.out.println("端口: 8006");
        System.out.println("========================================");
    }
}
