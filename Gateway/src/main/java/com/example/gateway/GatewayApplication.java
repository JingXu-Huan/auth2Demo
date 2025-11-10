package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-09
 * Gateway 网关应用启动类
 * 排除数据源自动配置，因为网关不需要数据库连接
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("========================================");
        System.out.println("网关服务启动成功！");
        System.out.println("端口: 9000");
        System.out.println("========================================");
    }
}