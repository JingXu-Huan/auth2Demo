package com.example.common.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 服务间认证配置
 * 用于生成和验证服务间调用的 JWT Token
 */
@Configuration
@Slf4j
@ConditionalOnClass(name = "io.jsonwebtoken.Jwts")
@ConditionalOnProperty(name = "service.auth.secret")
public class ServiceAuthConfig {
    
    @Value("${service.auth.secret}")
    private String serviceSecret;
    
    @Value("${spring.application.name:unknown-service}")
    private String serviceName;
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 获取签名密钥
     * 将配置的密钥字符串转换为SecretKey对象
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = serviceSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 生成服务间调用的 JWT Token
     * Token 有效期为 1 小时
     * @return JWT Token
     */
    public String generateServiceToken() {
        try {
            return Jwts.builder()
                .subject(serviceName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000)) // 1小时
                .claim("service", true)
                .claim("serviceType", serviceName)
                .signWith(getSigningKey())
                .compact();
        } catch (Exception e) {
            log.error("生成服务 Token 失败", e);
            return null;
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 验证服务间调用的 Token
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateServiceToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("服务 Token 验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 从 Token 中获取服务名称
     * @param token JWT Token
     * @return 服务名称
     */
    public String getServiceNameFromToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getPayload()
                .getSubject();
        } catch (Exception e) {
            log.warn("解析服务 Token 失败: {}", e.getMessage());
            return null;
        }
    }
}
