package com.example.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * User 服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServerApplication.class, args);
    }
}
