package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

/**
 * OAuth2 授权服务器启动类
 * 
 * @author Cascade
 * @date 2025-11-02
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan(basePackages = {"com.example.auth", "com.example.common"})
public class AuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("OAuth2 授权服务器启动成功！");
        System.out.println("端口: 8080");
        System.out.println("令牌端点: http://localhost:8080/oauth/token");
        System.out.println("API 文档: http://localhost:8080/doc.html");
        System.out.println("========================================");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

