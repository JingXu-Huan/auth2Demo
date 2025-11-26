package com.example.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * IM 推送服务启动类
 * 负责WebSocket长连接管理和消息推送
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.example.push", "com.example.common"})
public class ImPushServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImPushServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("IM 推送服务启动成功！");
        System.out.println("端口: 8004");
        System.out.println("WebSocket: ws://localhost:8004/ws/chat");
        System.out.println("========================================");
    }
}
