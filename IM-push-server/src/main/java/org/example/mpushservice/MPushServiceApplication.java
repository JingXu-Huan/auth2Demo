package org.example.mpushservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * IM 推送服务启动类
 * 排除数据源和 MongoDB 自动配置（本服务不需要数据库）
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    MongoAutoConfiguration.class
})
@EnableFeignClients
public class MPushServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MPushServiceApplication.class, args);
    }

}
