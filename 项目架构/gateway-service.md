作为整个系统的“咽喉”，网关不仅是流量入口，更是**安全防线（Security Perimeter）和流量治理中心（Traffic Governance）**。本设计将深度结合之前讨论的 **JWT 鉴权、Redis 封禁风控、Node.js WebSocket 转发** 等特性。

---

### 1. 核心定位与技术选型
+ **核心定位**：统一接入、身份验证、协议适配、流量清洗、熔断限流。
+ **技术栈**：
    - **核心框架**：Spring Cloud Gateway (基于 Spring WebFlux + Reactor Netty，全异步非阻塞)。
    - **注册中心**：Nacos (服务发现与动态配置)。
    - **限流熔断**：Alibaba Sentinel (网关流控适配器)。
    - **链路追踪**：Micrometer Tracing + Zipkin/SkyWalking。
    - **负载均衡**：Spring Cloud LoadBalancer。

---

### 2. 架构拓扑与过滤器链 (Filter Chain)
网关处理请求的核心逻辑在于**过滤器链**。

```mermaid
graph TD
    ClientRequest[客户端请求 HTTP/WS] --> Nginx
    Nginx --> Gateway[Gateway Service]
    
    subgraph GatewayFilters [Gateway 过滤器链]
        F1[TraceFilter: 生成 TraceID]
        F2[CorsFilter: 跨域处理]
        F3[RateLimitFilter: Sentinel 限流]
        F4[AuthFilter: JWT校验 & 风控检查]
        F5[GrayFilter: 灰度路由]
    end
    
    Gateway --> F1 --> F2 --> F3 --> F4 --> F5
    
    F5 -->|/api/auth| AuthService[Auth Service]
    F5 -->|/api/im| IMService[IM Service]
    F5 -->|/ws/doc| NodeSidecar[Collab Service (Node.js)]
    
    F4 -.-> Redis[Redis: 黑名单/踢人验证]
```

---

### 3. 核心功能模块详细设计
#### 3.1 统一鉴权过滤器 (`GlobalAuthFilter`)
这是网关最重的逻辑，必须实现**无状态（JWT 验签）**与**有状态（Redis 风控）**的结合。

+ **逻辑流程**：
    1. **白名单跳过**：检查请求路径（如 `/api/auth/login`, `/api/auth/code`），在白名单内则直接放行。
    2. **Token 提取**：从 Header (`Authorization: Bearer ...`) 或 Query Param (`?token=...` 用于 WebSocket) 中提取 Token。
    3. **本地验签 (CPU 密集)**：使用 RSA 公钥校验 JWT 签名和过期时间。失败直接返回 401。
    4. **黑名单/踢人检查 (IO 密集)**：
        * 从 JWT Payload 解析出 `user_id` 和 `device_id`。
        * **查询 Redis**：检查 `risk:ban:user:{uid}` (封号) 或 `auth:kick:{uid}:{device_id}` (踢下线)。
        * 如果存在 Key，说明用户被封禁或被踢，拦截请求并返回 403。
    5. **上下文传递**：将解析出的 `X-User-Id`、`X-Role` 放入 Request Header，供下游微服务直接使用（下游不再解析 JWT，只信赖网关传递的 Header）。

#### 3.2 动态路由与协议适配
网关需要处理 Java (HTTP) 和 Node.js (WebSocket) 的混合路由。

+ **路由配置文件 (**`application.yml`**)**：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 1. 认证服务路由
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1  # 去掉 /api 前缀

        # 2. IM 业务路由
        - id: im-service
          uri: lb://im-service
          predicates:
            - Path=/api/im/**
          filters:
            - StripPrefix=1

        # 3. 文档协同 (Node.js) WebSocket 路由
        # 注意: 使用 lb:ws:// 协议支持 WebSocket 负载均衡
        - id: collab-service-ws
          uri: lb:ws://collab-service
          predicates:
            - Path=/doc/ws/**
          filters:
             # WebSocket 握手时通常带 token 参数，需配合 AuthFilter 处理
```

#### 3.3 流量控制与熔断 (Sentinel)
企业级系统必须防止“雪崩”。

+ **IP 频控**：单 IP 每秒限制 50 QPS，防止爬虫。
+ **用户频控**：单 UserID 每秒限制 10 次写请求（POST/PUT），防止恶意刷屏。
+ **热点参数限流**：针对特定的 API（如获取验证码接口），限制同一个手机号/邮箱的请求频率。
+ **配置方式**：使用 Sentinel 控制台动态下发规则至 Nacos，网关实时生效。

#### 3.4 灰度发布 (Gray Release)
为了支持“金丝雀发布”（Canary Release），网关需要根据用户特征分流。

+ **策略**：
    - **Header 路由**：请求头带 `X-Version: v2` 的转发到灰度实例。
    - **权重路由**：配置 `v1` 服务权重 90%，`v2` 服务权重 10%。
    - **用户 ID 取模**：自定义 `GrayLoadBalancer`，根据 `userId % 100 < 5` (5%流量) 转发到新服务。

---

### 4. 关键代码实现 (Java)
#### 4.1 全局鉴权过滤器 (`AuthGlobalFilter.java`)
```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtVerifier jwtVerifier; // JWT 工具类
    @Autowired
    private StringRedisTemplate redisTemplate; // Redis

    // 白名单路径
    private static final List<String> SKIP_URLS = Arrays.asList(
        "/api/auth/login", "/api/auth/register", "/api/auth/code"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // 1. 白名单检查
        if (isSkipUrl(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Token
        String token = extractToken(exchange);
        if (token == null) {
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Missing Token");
        }

        try {
            // 3. 本地验签 (JWT)
            Claims claims = jwtVerifier.verify(token);
            String userId = claims.getSubject();
            String deviceId = claims.get("deviceId", String.class);

            // 4. 风控与踢下线检查 (Redis)
            // 检查全局封禁
            if (redisTemplate.hasKey("risk:ban:user:" + userId)) {
                return errorResponse(exchange, HttpStatus.FORBIDDEN, "Account Banned");
            }
            // 检查特定设备踢下线
            if (redisTemplate.hasKey("auth:kick:" + userId + ":" + deviceId)) {
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Device Kicked Out");
            }
            // 检查 Token 是否已注销 (Logout)
            // (Access Token未过期但用户点了注销，需在Redis记黑名单)
            if (redisTemplate.hasKey("auth:block:token:" + token)) {
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Token Invalidated");
            }

            // 5. 传递 UserID 给下游
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-Device-Id", deviceId)
                .build();
            
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid Token");
        }
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus status, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + status.value() + ", \"message\":\"" + msg + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // 优先级较高，在 NettyRoutingFilter 之前
    }
}
```

#### 4.2 全局异常处理 (`GlobalErrorExceptionHandler.java`)
Spring Cloud Gateway 的异常处理比较特殊，需要接管 `ErrorWebExceptionHandler`。

+ **职责**：将网关内部的 `NotFoundException` (404), `ConnectException` (下游挂了), `SentinelBlockException` (限流) 统一转化为前端可读的 JSON 格式。
+ **输出格式**：

```json
{
  "code": 429,
  "message": "Too Many Requests (Rate Limited by Sentinel)",
  "data": null
}
```

---

### 5. 性能与高可用配置
#### 5.1 Netty 调优
由于网关是全异步的，Netty 的配置决定了吞吐量上限。

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 3000
        response-timeout: 10s
        pool:
          type: ELASTIC       # 弹性连接池
          max-idle-time: 30s  # 空闲回收
```

#### 5.2 跨域配置 (CORS)
前后端分离必配。

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*" # 生产环境建议指定具体域名
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
```

### 6. 安全加固 (Security Hardening)
1. **Request Body 大小限制**：防止大文件上传攻击打爆网关内存。

```yaml
spring.codec.max-in-memory-size: 10MB
```

2. **隐藏敏感 Header**：不在 Response 中暴露 `Server: Netty`, `X-Powered-By` 等信息。
3. **SQL 注入初筛**：可以在网关层加一个简单的 Regex Filter，检测 URL 参数中是否包含 `DROP TABLE`, `DELETE FROM` 等极其明显的攻击特征（深度防御还得靠 WAF）。

### 总结
这个 `gateway-service` 设计不仅仅是转发请求，它实际上承担了你系统中 **Auth Check Point (鉴权点)** 和 **Risk Enforcement Point (风控执行点)** 的角色。通过集成 Redis 检查，它完美响应了之前数据库设计中的 `user_punishments` 和 `user_devices` 表逻辑。

