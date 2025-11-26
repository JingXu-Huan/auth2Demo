package com.example.relationship;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 好友关系服务启动类
 */
@SpringBootApplication(excludeName = {"com.alibaba.cloud.sentinel.feign.SentinelFeignAutoConfiguration"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@MapperScan("com.example.relationship.mapper")
public class RelationshipServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RelationshipServerApplication.class, args);
    }
}
