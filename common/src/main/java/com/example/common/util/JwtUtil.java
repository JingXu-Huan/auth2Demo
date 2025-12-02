package com.example.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ====================================================================
 * JWT (JSON Web Token) 工具类
 * ====================================================================
 * 
 * 【JWT简介】
 * JWT是一种开放标准（RFC 7519），用于在各方之间安全传输信息。
 * 它由三部分组成，用点(.)分隔：
 * 
 *   Header.Payload.Signature
 *   头部.负载.签名
 * 
 * 【JWT结构详解】
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Header (头部) - Base64编码                                  │
 * │ {                                                           │
 * │   "alg": "HS256",    // 签名算法                            │
 * │   "typ": "JWT"       // Token类型                           │
 * │ }                                                           │
 * ├─────────────────────────────────────────────────────────────┤
 * │ Payload (负载) - Base64编码                                 │
 * │ {                                                           │
 * │   "userId": 123,          // 自定义声明                     │
 * │   "username": "admin",    // 自定义声明                     │
 * │   "iat": 1234567890,      // 签发时间                       │
 * │   "exp": 1234654290       // 过期时间                       │
 * │ }                                                           │
 * ├─────────────────────────────────────────────────────────────┤
 * │ Signature (签名) - 防篡改                                    │
 * │ HMACSHA256(                                                  │
 * │   base64UrlEncode(header) + "." +                           │
 * │   base64UrlEncode(payload),                                 │
 * │   secret                                                     │
 * │ )                                                            │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * 【使用流程】
 * 1. 用户登录成功 → 服务器生成JWT → 返回给客户端
 * 2. 客户端存储JWT（localStorage或Cookie）
 * 3. 后续请求携带JWT（通常在Authorization头：Bearer <token>）
 * 4. 服务器验证JWT签名和有效期
 * 
 * 【安全注意事项】
 * - 密钥(secret)必须保密，建议使用环境变量
 * - 不要在Payload中存放敏感信息（如密码）
 * - 设置合理的过期时间
 * - 生产环境建议使用HTTPS
 * 
 * @author 学习笔记
 * @see io.jsonwebtoken.Jwts JJWT库核心类
 */
@Slf4j
@Component
@ConditionalOnClass(name = "io.jsonwebtoken.Jwts")  // 只有存在JJWT库时才创建Bean
public class JwtUtil {
    
    /**
     * JWT签名密钥
     * 
     * 【@Value注解说明】
     * - 从application.yml读取配置值
     * - 冒号后面是默认值，配置不存在时使用
     * - 生产环境应使用环境变量：${JWT_SECRET}
     * 
     * 【密钥要求】
     * - HS256算法要求密钥至少256位（32字节）
     * - 密钥越长越安全
     */
    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secret;
    
    /**
     * Token有效期（毫秒）
     * 默认24小时 = 24 * 60 * 60 * 1000 = 86400000
     */
    @Value("${jwt.expiration:86400000}")
    private Long expiration;
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 生成 JWT token
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT token
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return generateToken(claims);
    }
    
    /**
     * 生成 JWT token（包含邮箱验证状态）
     * @param userId 用户ID
     * @param username 用户名
     * @param emailVerified 邮箱是否已验证
     * @return JWT token
     */
    public String generateToken(Long userId, String username, Boolean emailVerified) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("email_verified", emailVerified != null ? emailVerified : false);
        return generateToken(claims);
    }
    
    /**
     * 生成 JWT token（带自定义 claims）
     * @param claims 自定义声明
     * @return JWT token
     */
    public String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * 从 token 中解析 Claims
     * @param token JWT token
     * @return Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("解析 token 失败", e);
            return null;
        }
    }
    
    /**
     * 从 token 中获取用户ID
     * @param token JWT token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }
    
    /**
     * 从 token 中获取用户名
     * @param token JWT token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("username") : null;
    }
    
    /**
     * 验证 token 是否有效
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return false;
            }
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error("验证 token 失败", e);
            return false;
        }
    }
    
    /**
     * 检查 token 是否过期
     * @param token JWT token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return true;
            }
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * 刷新 token
     * @param token 旧 token
     * @return 新 token
     */
    public String refreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return null;
            }
            // 创建新的 claims Map
            Map<String, Object> newClaims = new HashMap<>(claims);
            return generateToken(newClaims);
        } catch (Exception e) {
            log.error("刷新 token 失败", e);
            return null;
        }
    }
}
