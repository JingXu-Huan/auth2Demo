package com.example.av;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 音视频服务启动类
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}, 
    excludeName = {"com.alibaba.cloud.sentinel.feign.SentinelFeignAutoConfiguration"})
@EnableDiscoveryClient
@EnableFeignClients
public class AvServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvServerApplication.class, args);
    }
}
