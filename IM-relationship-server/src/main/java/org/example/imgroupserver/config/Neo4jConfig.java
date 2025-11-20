package org.example.imgroupserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Neo4j 配置类
 * 确保 Neo4j 仓库扫描和事务管理正常启用
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "org.example.imgroupserver.mapper")
@EnableTransactionManagement
public class Neo4jConfig {
    // Neo4j 连接配置已在 application.yml 中定义
    // 本配置类主要用于显式启用仓库扫描和事务管理
}
