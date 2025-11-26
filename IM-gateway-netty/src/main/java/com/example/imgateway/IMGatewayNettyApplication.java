package com.example.imgateway;

import com.example.imgateway.server.NettyServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * IM-Gateway-Netty 应用启动类
 * 基于Netty的高性能长连接网关，支持C10K/C100K
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.imgateway",  // 扫描当前模块
    "com.example.common"       // 扫描 common 模块（包含 JwtUtil 等工具类）
})
public class IMGatewayNettyApplication implements CommandLineRunner {

    @Autowired
    private NettyServer nettyServer;

    public static void main(String[] args) {
        SpringApplication.run(IMGatewayNettyApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("===========================================");
        log.info("IM-Gateway-Netty Starting...");
        log.info("===========================================");
        
        // 启动Netty Server（异步）
        nettyServer.start();
        
        log.info("===========================================");
        log.info("IM-Gateway-Netty Started Successfully!");
        log.info("===========================================");
    }
}
