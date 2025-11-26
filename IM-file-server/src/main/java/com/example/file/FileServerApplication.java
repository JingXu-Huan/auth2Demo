package com.example.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 文件存储服务启动类
 * 基于CAS（内容寻址存储）实现文件去重
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@MapperScan("com.example.file.mapper")
@ComponentScan(basePackages = {"com.example.file", "com.example.common"})
public class FileServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("文件存储服务启动成功！");
        System.out.println("端口: 8005");
        System.out.println("========================================");
    }
}
