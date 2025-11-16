package com.example.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * User 服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync  
@ComponentScan(basePackages = {"com.example.user", "com.example.common"})
public class UserServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServerApplication.class, args);
    }
}
