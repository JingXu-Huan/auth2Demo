package com.example.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Email 服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.example.email", "com.example.common"})
public class EmailServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(EmailServerApplication.class, args);
    }
}
