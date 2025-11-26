package com.example.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务服务启动类
 */
@SpringBootApplication(excludeName = {"com.alibaba.cloud.sentinel.feign.SentinelFeignAutoConfiguration"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class JobServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}
