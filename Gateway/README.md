# OAuth2 Gateway 认证全流程文档

## 目录

- [系统架构](#系统架构)
- [认证流程](#认证流程)
- [服务间认证](#服务间认证)
- [过滤器链](#过滤器链)
- [路由配置](#路由配置)
- [安全机制](#安全机制)

---

## 系统架构

```
┌─────────────┐
│   用户浏览器   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│         Gateway (端口: 9000)         │
│  ┌─────────────────────────────┐   │
│  │  AuthFilter (Order: -100)   │   │  ← 用户认证
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ ServiceAuthGatewayFilter    │   │  ← 服务间认证
│  │      (Order: -99)           │   │
│  └─────────────────────────────┘   │
└───────┬──────────────┬──────────────┘
        │              │
        ▼              ▼
┌──────────────┐  ┌──────────────┐
│ OAuth2-Auth  │  │ User-Server  │
│   Server     │  │ (端口: 8082) │
│ (端口: 8080) │  └──────────────┘
└──────────────┘
```

---

## 认证流程

### 1. 用户注册流程

```
用户 → Gateway → User-Server
  │
  ├─ POST /api/users/register
  │   ├─ 用户信息验证
  │   ├─ 密码加密存储
  │   └─ 发送验证邮件
  │
  └─ GET /api/users/confirm?token=xxx
      └─ 激活用户账户
```

**详细步骤:**

1. 用户提交注册信息到 `/api/users/register`
2. Gateway 路由到 User-Server（公开接口，无需认证）
3. User-Server 验证信息并创建用户（状态: INACTIVE）
4. 发送验证邮件到用户邮箱
5. 用户点击邮件链接访问 `/api/users/confirm?token=xxx`
6. User-Server 验证 token 并激活账户

### 2. 用户登录流程（OAuth2 密码模式）

```
用户 → Gateway → OAuth2-Auth-Server → User-Server
  │                     │                    │
  │  POST /oauth/token  │                    │
  │  ─────────────────> │                    │
  │                     │  Feign调用         │
  │                     │  getUserDetails    │
  │                     │  ─────────────────>│
  │                     │  (带X-Service-Auth)│
  │                     │                    │
  │                     │  <─────────────────│
  │                     │  返回用户详情       │
  │                     │                    │
  │  <─────────────────│                    │
  │  返回 access_token  │                    │
```

**详细步骤:**

1. 用户提交登录请求到 `/oauth/token`
   ```
   POST /oauth/token
   Content-Type: application/x-www-form-urlencoded
   
   grant_type=password
   username=user@example.com
   password=123456
   client_id=client
   client_secret=secret
   ```

2. Gateway 转发到 OAuth2-Auth-Server（公开接口）

3. OAuth2-Auth-Server 处理流程:
   - LoginAttemptFilter 检查账户是否被锁定
   - 验证 client_id 和 client_secret
   - 通过 Feign 调用 User-Server 获取用户详情
   - Feign 请求自动添加 `X-Service-Auth` header（ServiceAuthInterceptor）
   - 验证用户密码
   - 生成 access_token 和 refresh_token

4. 返回 token 给用户:
   ```json
   {
     "access_token": "eyJhbGciOiJIUzI1NiIs...",
     "token_type": "bearer",
     "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
     "expires_in": 3600,
     "scope": "read write"
   }
   ```

### 3. 访问受保护资源流程

```
用户 → Gateway → User-Server/Resource-Server
  │         │            │
  │  GET    │            │
  │  /api/  │  验证       │
  │  users  │  Token     │
  │  ─────> │  ────────> │
  │  Bearer │  添加服务   │
  │  Token  │  认证Token │
  │         │            │
  │  <───────────────────│
  │  返回用户数据         │
```

**详细步骤:**

1. 用户携带 access_token 访问受保护接口
   ```
   GET /api/users/123
   Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
   ```

2. Gateway 处理流程:
   - **AuthFilter** (Order: -100): 验证 Bearer Token 是否存在
   - **ServiceAuthGatewayFilter** (Order: -99): 
     - 检查是否是内部接口
     - 如果是内部接口，添加 `X-Service-Auth` header
   - TokenRelay Filter: 转发用户的 Bearer Token

3. User-Server 处理流程:
   - **ServiceAuthFilter** (Order: 1): 验证服务间认证 token
   - Spring Security: 验证用户 OAuth2 token
   - 返回请求的资源

### 4. 第三方登录流程（Gitee OAuth2）

```
用户 → Gateway → OAuth2-Auth-Server → Gitee
  │                     │                │
  │  GET /oauth/gitee   │                │
  │  ─────────────────> │  重定向到Gitee  │
  │                     │  ─────────────>│
  │                     │                │
  │  <──────────────────────────────────│
  │  Gitee授权页面                       │
  │                     │                │
  │  用户授权           │                │
  │  ─────────────────────────────────> │
  │                     │                │
  │  callback?code=xxx  │  获取token     │
  │  ─────────────────> │  ─────────────>│
  │                     │  获取用户信息   │
  │                     │  ─────────────>│
  │                     │                │
  │                     │  创建/更新用户  │
  │                     │  (Feign调用)   │
  │                     │  ───────────> User-Server
  │                     │                │
  │  <─────────────────│                │
  │  返回 access_token  │                │
```

**详细步骤:**

1. 用户访问 `/oauth/gitee`
2. 重定向到 Gitee 授权页面
3. 用户在 Gitee 授权
4. Gitee 回调到 `/oauth/gitee/callback?code=xxx`
5. OAuth2-Auth-Server 使用 code 换取 Gitee access_token
6. 获取 Gitee 用户信息
7. 通过 Feign 调用 User-Server 创建或更新用户
8. 生成系统 access_token 返回给用户

---

## 服务间认证

### 认证机制

所有服务间调用使用 JWT Token 进行认证，通过 `X-Service-Auth` header 传递。

### Token 生成

**ServiceAuthConfig.java** (common 模块):

```java
public String generateServiceToken() {
    return Jwts.builder()
        .subject(serviceName)                    // 服务名称
        .issuedAt(new Date())                    // 签发时间
        .expiration(new Date(System.currentTimeMillis() + 3600000))  // 1小时过期
        .claim("service", true)                  // 服务标识
        .claim("serviceType", serviceName)       // 服务类型
        .signWith(getSigningKey())               // 使用密钥签名
        .compact();
}
```

### Token 验证

**ServiceAuthConfig.java**:

```java
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
```

### 服务间调用场景

#### 场景1: OAuth2-Auth-Server → User-Server

**调用方式:** Feign Client

**拦截器:** ServiceAuthInterceptor

```java
@Override
public void apply(RequestTemplate template) {
    String token = authConfig.generateServiceToken();
    if (token != null) {
        template.header("X-Service-Auth", token);
    }
}
```

**示例调用:**
```java
@FeignClient(name = "user-server")
public interface UserServiceClient {
    @GetMapping("/api/users/details/email/{email}")
    UserDetailsDTO getUserDetailsByEmail(@PathVariable("email") String email);
}
```

#### 场景2: Gateway → User-Server

**调用方式:** HTTP 转发

**过滤器:** ServiceAuthGatewayFilter

```java
if (isInternalApi) {
    String serviceToken = authConfig.generateServiceToken();
    ServerHttpRequest modifiedRequest = exchange.getRequest()
        .mutate()
        .header("X-Service-Auth", serviceToken)
        .build();
}
```

### 受保护的内部接口

**User-Server 内部接口:**

- `/api/users/details/**` - 用户详情接口（包含敏感信息）
- `/api/users/internal/**` - 其他内部接口

**验证逻辑 (ServiceAuthFilter):**

```java
if (isInternalApi) {
    String serviceToken = request.getHeader("X-Service-Auth");
    
    if (serviceToken == null || !authConfig.validateServiceToken(serviceToken)) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write("{\"code\":403,\"message\":\"访问被拒绝：无效的服务认证\"}");
        return;
    }
}
```

---

## 过滤器链

### Gateway 过滤器执行顺序

```
请求进入
  │
  ▼
┌─────────────────────────────────┐
│ AuthFilter (Order: -100)        │  ← 验证用户 Bearer Token
│ - 检查 Authorization header     │
│ - 公开接口直接放行              │
│ - 受保护接口验证 token 存在     │
└─────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────┐
│ ServiceAuthGatewayFilter        │  ← 添加服务间认证
│ (Order: -99)                    │
│ - 公开接口跳过                  │
│ - 内部接口添加 X-Service-Auth   │
└─────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────┐
│ TokenRelay Filter               │  ← 转发用户 Token
│ - 转发 Authorization header     │
└─────────────────────────────────┘
  │
  ▼
转发到后端服务
```

### User-Server 过滤器执行顺序

```
请求进入
  │
  ▼
┌─────────────────────────────────┐
│ ServiceAuthFilter (Order: 1)    │  ← 验证服务间认证
│ - 公开接口直接放行              │
│ - 内部接口验证 X-Service-Auth   │
└─────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────┐
│ Spring Security Filter Chain    │  ← 验证用户认证
│ - OAuth2 Resource Server        │
│ - 验证 Bearer Token             │
└─────────────────────────────────┘
  │
  ▼
Controller 处理业务逻辑
```

### OAuth2-Auth-Server 过滤器执行顺序

```
请求进入
  │
  ▼
┌─────────────────────────────────┐
│ LoginAttemptFilter (Order: 1)   │  ← 防暴力破解
│ - 检查账户是否被锁定            │
│ - 只拦截 /oauth/token           │
└─────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────┐
│ Spring Security Filter Chain    │  ← OAuth2 认证
│ - OAuth2 Authorization Server   │
│ - 验证 client 和 用户凭证       │
└─────────────────────────────────┘
  │
  ▼
生成并返回 Token
```

---

## 路由配置

### 公开接口路由（无需认证）

#### 1. OAuth2 认证接口
```yaml
- id: auth-server
  uri: http://localhost:8080
  predicates:
    - Path=/oauth/**,/login.html,/api/auth/**
```

**包含接口:**
- `/oauth/token` - 获取 token
- `/oauth/authorize` - 授权端点
- `/oauth/gitee` - Gitee 第三方登录
- `/login.html` - 登录页面

#### 2. 用户注册接口
```yaml
- id: user-register
  uri: http://localhost:8082
  predicates:
    - Path=/api/users/register
```

#### 3. 邮箱验证接口
```yaml
- id: user-confirm
  uri: http://localhost:8082
  predicates:
    - Path=/api/users/confirm
```

#### 4. 邮箱检查接口
```yaml
- id: user-email-check
  uri: http://localhost:8082
  predicates:
    - Path=/api/users/exists/email/**
```

#### 5. 安全验证接口
```yaml
- id: security-verification
  uri: http://localhost:8082
  predicates:
    - Path=/api/security/**
```

**包含接口:**
- `/api/security/captcha/send` - 发送验证码
- `/api/security/captcha/verify` - 验证验证码

### 受保护接口路由（需要认证）

#### 1. 用户服务接口
```yaml
- id: user-server
  uri: http://localhost:8082
  predicates:
    - Path=/api/**
  filters:
    - TokenRelay
```

**需要 Bearer Token**

#### 2. 资源服务器接口
```yaml
- id: resource-server
  uri: http://localhost:8081
  predicates:
    - Path=/resource/**
  filters:
    - TokenRelay
```

**需要 Bearer Token**

---

## 安全机制

### 1. 用户认证安全

#### OAuth2 Token 机制
- **access_token**: 访问令牌，有效期 1 小时
- **refresh_token**: 刷新令牌，用于获取新的 access_token
- **token_type**: Bearer
- **scope**: 权限范围（read, write）

#### 密码安全
- 使用 BCrypt 加密存储
- 加密强度: 10 轮
- 不可逆加密

#### 登录保护
- 失败次数限制: 5 次
- 锁定时间: 15 分钟
- 基于用户名的锁定策略

### 2. 服务间认证安全

#### JWT Token 机制
- **算法**: HMAC-SHA256
- **密钥长度**: 至少 32 位
- **有效期**: 1 小时
- **签发者**: 服务名称

#### 密钥配置
```yaml
service:
  auth:
    secret: your_32_character_secret_key_here_12345
```

**重要:** 所有服务必须使用相同的密钥

### 3. 接口访问控制

#### 公开接口
- 用户注册
- 邮箱验证
- OAuth2 认证
- 验证码发送/验证

#### 内部接口（需要服务认证）
- `/api/users/details/**` - 用户详情（包含密码等敏感信息）
- `/api/users/internal/**` - 内部管理接口

#### 受保护接口（需要用户认证）
- `/api/users/**` - 用户管理接口
- `/resource/**` - 资源访问接口

### 4. CORS 安全

配置在 `CorsGlobalConfiguration.java`:
- 允许的源: 配置的白名单
- 允许的方法: GET, POST, PUT, DELETE, OPTIONS
- 允许的 Headers: 所有
- 允许凭证: true

### 5. 防护措施

#### 防暴力破解
- LoginAttemptFilter 限制登录尝试
- Redis 存储失败次数
- 自动锁定机制

#### 防重放攻击
- JWT Token 包含时间戳
- Token 有过期时间
- 每次请求验证 Token 有效性

#### 防 CSRF
- 使用 Bearer Token 而非 Cookie
- 无状态认证机制

---

## 配置说明

### Gateway 配置

**application.yml:**
```yaml
server:
  port: 9000

spring:
  application:
    name: oauth2-gateway
  cloud:
    nacos:
      discovery:
        server-addr: 154.219.109.125:8848
    gateway:
      routes:
        # 路由配置...

service:
  auth:
    secret: your_32_character_secret_key_here_12345
```

### 依赖配置

**pom.xml:**
```xml
<!-- Spring Cloud Gateway -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- OAuth2 Client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- Common module for service authentication -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>common</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 测试示例

### 1. 用户注册

```bash
curl -X POST http://localhost:9000/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 2. 用户登录

```bash
curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=test@example.com&password=password123&client_id=client&client_secret=secret"
```

### 3. 访问受保护资源

```bash
curl -X GET http://localhost:9000/api/users/123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

### 4. 刷新 Token

```bash
curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&refresh_token=eyJhbGciOiJIUzI1NiIs...&client_id=client&client_secret=secret"
```

---

## 故障排查

### 常见问题

#### 1. 401 Unauthorized
- 检查 Bearer Token 是否正确
- 检查 Token 是否过期
- 检查 Authorization header 格式

#### 2. 403 Forbidden (服务间认证失败)
- 检查服务间认证密钥是否一致
- 检查 X-Service-Auth header 是否存在
- 检查 ServiceAuthGatewayFilter 是否正常工作

#### 3. 账户被锁定
- 等待 15 分钟后重试
- 或联系管理员解锁

#### 4. Token 无法刷新
- 检查 refresh_token 是否有效
- 检查 client_id 和 client_secret

---

## 总结

本系统实现了完整的 OAuth2 认证流程，包括:

1. **用户认证**: OAuth2 密码模式 + 第三方登录（Gitee）
2. **服务间认证**: JWT Token 机制
3. **多层安全防护**: 
   - Gateway 层: 用户 Token 验证 + 服务认证添加
   - 服务层: 服务认证验证 + 用户权限验证
4. **防护机制**: 防暴力破解、防重放攻击、CORS 保护

所有认证流程都经过严格的安全验证，确保系统的安全性和可靠性。
