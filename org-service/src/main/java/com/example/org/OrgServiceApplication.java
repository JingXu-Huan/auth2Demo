package com.example.org;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 组织架构服务启动类
 */
@SpringBootApplication(excludeName = {"com.alibaba.cloud.sentinel.feign.SentinelFeignAutoConfiguration"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@MapperScan("com.example.org.mapper")
public class OrgServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrgServiceApplication.class, args);
    }
}
