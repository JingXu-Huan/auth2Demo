package com.example.im;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * IM 消息服务启动类
 * 负责消息的发送、存储、同步
 */
@SpringBootApplication(excludeName = {"com.alibaba.cloud.sentinel.feign.SentinelFeignAutoConfiguration"})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.example.im.mapper")
@ComponentScan(basePackages = {"com.example.im", "com.example.common"})
public class ImMessageServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImMessageServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("IM 消息服务启动成功！");
        System.out.println("端口: 8002");
        System.out.println("========================================");
    }
}
