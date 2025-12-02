package com.example.gateway.filter;

import com.example.gateway.util.JwtVerifier;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * ====================================================================
 * 全局鉴权过滤器 (Gateway Global Authentication Filter)
 * ====================================================================
 * 
 * 【核心作用】
 * 作为微服务网关的第一道防线，负责：
 * 1. JWT Token 验证 - 确保请求携带有效的认证令牌
 * 2. 用户信息传递 - 将用户ID等信息传递给下游服务
 * 3. 风控检查 - 通过Redis检查用户是否被封禁
 * 
 * 【请求处理流程】
 * ┌─────────────────────────────────────────────────────────────┐
 * │  客户端请求                                                  │
 * │      ↓                                                      │
 * │  Gateway 网关接收                                            │
 * │      ↓                                                      │
 * │  AuthGlobalFilter（本类）                                    │
 * │      ├─ 白名单检查 → 跳过认证                                │
 * │      ├─ 提取Token                                           │
 * │      ├─ JWT验签                                              │
 * │      ├─ Redis风控检查                                        │
 * │      └─ 添加用户信息到请求头                                 │
 * │      ↓                                                      │
 * │  路由到下游微服务                                            │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * 【核心接口说明】
 * GlobalFilter - Spring Cloud Gateway 的全局过滤器接口
 * Ordered      - 定义过滤器执行顺序（数值越小优先级越高）
 * 
 * 【响应式编程】
 * Gateway基于WebFlux（响应式编程模型），使用Mono/Flux：
 * - Mono<T> - 0或1个元素的异步序列
 * - Flux<T> - 0到N个元素的异步序列
 * - 所有操作都是非阻塞的
 * 
 * @author 学习笔记
 * @see GlobalFilter Spring Cloud Gateway全局过滤器
 * @see JwtVerifier JWT验证工具
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** JWT验证工具 - 负责Token的解析和验证 */
    @Autowired
    private JwtVerifier jwtVerifier;

    /**
     * 响应式Redis模板
     * 
     * 【响应式Redis说明】
     * - Gateway使用WebFlux，必须用响应式Redis客户端
     * - required=false: Redis不可用时不影响启动
     * - 用于检查用户封禁状态、Token黑名单等
     */
    @Autowired(required = false)
    private ReactiveStringRedisTemplate redisTemplate;

    /**
     * Ant风格路径匹配器
     * 
     * 支持的通配符：
     * - ? 匹配单个字符
     * - * 匹配0个或多个字符（不含路径分隔符）
     * - ** 匹配0个或多个目录
     * 
     * 示例：/api/** 匹配 /api/users, /api/v1/users 等
     */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 白名单路径（不需要认证）
     */
    private static final List<String> SKIP_URLS = Arrays.asList(
            // 认证相关
            "/oauth/**",
            "/login",
            "/login.html",
            "/api/auth/**",
            "/api/v1/auth/**",
            "/api/oauth/**",
            
            // 用户注册/验证
            "/api/users/register",
            "/api/users/confirm",
            "/api/users/check-email",
            "/api/users/check-username",
            "/api/v1/users/register",
            "/api/v1/users/confirm",
            "/api/v1/users/check-email",
            "/api/v1/users/check-username",
            "/api/v1/users/details/email/**",
            "/api/v1/users/update-login-time",
            
            // 邮件验证
            "/api/email/**",
            "/api/v1/email/**",
            
            // 安全验证码
            "/api/security/**",
            "/api/v1/security/**",
            
            // Actuator
            "/actuator/**",
            "/health/**",
            
            // Swagger/API文档
            "/doc.html",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/webjars/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 白名单检查
        if (isSkipUrl(path)) {
            log.debug("跳过认证: {}", path);
            return chain.filter(exchange);
        }

        // 2. 提取 Token
        String token = extractToken(exchange);
        if (!StringUtils.hasText(token)) {
            log.warn("缺少Token: {}", path);
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Missing Token");
        }

        try {
            // 3. 本地验签 (JWT)
            Claims claims = jwtVerifier.verify(token);
            String userId = getUserIdFromClaims(claims);
            String deviceId = claims.get("deviceId", String.class);

            if (userId == null) {
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid Token: missing userId");
            }

            // 4. 风控检查 (Redis)
            if (redisTemplate != null) {
                return checkRiskControl(exchange, chain, token, userId, deviceId, claims);
            }

            // 5. 传递用户信息给下游服务
            return forwardWithUserInfo(exchange, chain, userId, deviceId, claims);

        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid Token");
        }
    }

    /**
     * 风控检查（封禁、踢下线、Token黑名单）
     */
    private Mono<Void> checkRiskControl(ServerWebExchange exchange, GatewayFilterChain chain,
                                        String token, String userId, String deviceId, Claims claims) {
        // 检查用户封禁
        String banKey = "risk:ban:user:" + userId;
        
        return redisTemplate.hasKey(banKey)
                .flatMap(banned -> {
                    if (Boolean.TRUE.equals(banned)) {
                        log.warn("用户已被封禁: userId={}", userId);
                        return errorResponse(exchange, HttpStatus.FORBIDDEN, "Account Banned");
                    }
                    
                    // 检查设备踢下线
                    if (StringUtils.hasText(deviceId)) {
                        String kickKey = "auth:kick:" + userId + ":" + deviceId;
                        return redisTemplate.hasKey(kickKey)
                                .flatMap(kicked -> {
                                    if (Boolean.TRUE.equals(kicked)) {
                                        log.warn("设备已被踢下线: userId={}, deviceId={}", userId, deviceId);
                                        return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Device Kicked Out");
                                    }
                                    
                                    // 检查 Token 黑名单
                                    String blockKey = "auth:block:token:" + token.hashCode();
                                    return redisTemplate.hasKey(blockKey)
                                            .flatMap(blocked -> {
                                                if (Boolean.TRUE.equals(blocked)) {
                                                    log.warn("Token 已失效: userId={}", userId);
                                                    return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Token Invalidated");
                                                }
                                                return forwardWithUserInfo(exchange, chain, userId, deviceId, claims);
                                            });
                                });
                    }
                    
                    return forwardWithUserInfo(exchange, chain, userId, deviceId, claims);
                });
    }

    /**
     * 转发请求并附加用户信息
     */
    private Mono<Void> forwardWithUserInfo(ServerWebExchange exchange, GatewayFilterChain chain,
                                           String userId, String deviceId, Claims claims) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header("X-User-Id", userId);
        
        if (StringUtils.hasText(deviceId)) {
            requestBuilder.header("X-Device-Id", deviceId);
        }
        
        // 传递角色信息
        Object role = claims.get("role");
        if (role != null) {
            requestBuilder.header("X-User-Role", role.toString());
        }
        
        // 传递邮箱验证状态
        Object emailVerified = claims.get("email_verified");
        if (emailVerified != null) {
            requestBuilder.header("X-Email-Verified", emailVerified.toString());
        }

        ServerHttpRequest mutatedRequest = requestBuilder.build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 检查是否为白名单路径
     */
    private boolean isSkipUrl(String path) {
        return SKIP_URLS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 从请求中提取 Token
     */
    private String extractToken(ServerWebExchange exchange) {
        // 从 Authorization Header 提取
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 从 Query Parameter 提取（用于 WebSocket）
        String tokenParam = exchange.getRequest().getQueryParams().getFirst("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }

    /**
     * 从 Claims 中获取用户ID
     */
    private String getUserIdFromClaims(Claims claims) {
        // 尝试从 userId 字段获取
        Object userId = claims.get("userId");
        if (userId != null) {
            return userId.toString();
        }
        // 尝试从 subject 获取
        return claims.getSubject();
    }

    /**
     * 返回错误响应
     */
    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String body = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}", 
                status.value(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级
    }
}
