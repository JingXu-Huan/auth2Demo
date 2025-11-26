package com.example.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 验证工具类
 * 用于网关层验证 JWT Token
 */
@Slf4j
@Component
public class JwtVerifier {

    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secret;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 验证 Token 并返回 Claims
     * @param token JWT Token
     * @return Claims
     * @throws Exception 验证失败时抛出异常
     */
    public Claims verify(String token) throws Exception {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户ID
     */
    public String getUserId(String token) {
        try {
            Claims claims = verify(token);
            Object userId = claims.get("userId");
            return userId != null ? userId.toString() : claims.getSubject();
        } catch (Exception e) {
            log.error("获取用户ID失败", e);
            return null;
        }
    }

    /**
     * 从 Token 中获取设备ID
     */
    public String getDeviceId(String token) {
        try {
            Claims claims = verify(token);
            return claims.get("deviceId", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查 Token 是否过期
     */
    public boolean isExpired(String token) {
        try {
            Claims claims = verify(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
