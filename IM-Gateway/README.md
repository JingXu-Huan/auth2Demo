# IM-Gateway - 即时通讯网关

## 📋 项目概述

IM-Gateway 是即时通讯系统的统一网关，负责路由管理、负载均衡、限流控制和服务间认证。

## 🚀 功能特性

### 核心功能
- **WebSocket 路由**: 支持 WebSocket 长连接的负载均衡
- **API 网关**: 统一的 REST API 入口
- **服务间认证**: 基于 JWT 的服务间安全认证
- **限流控制**: 基于 Redis 的分布式限流
- **健康检查**: 完整的服务健康监控

### 路由配置
- `ws/**` - WebSocket 连接路由到 IM-message-server
- `api/chat/**` - 公开聊天 API 路由
- `internal/**` - 内部 API 路由（需要服务间认证）
- `actuator/**` - 健康检查路由
- `gateway/**` - 网关自身管理接口

## 🔧 技术栈

- **Spring Cloud Gateway**: 响应式网关框架
- **Spring Boot WebFlux**: 响应式 Web 框架
- **Redis**: 限流存储和缓存
- **Nacos**: 服务发现和配置管理
- **JWT**: 服务间认证

## 📦 依赖服务

- **IM-message-server**: 消息服务 (端口: 8002)
- **Redis**: 限流和缓存 (端口: 6379)
- **Nacos**: 服务注册中心 (端口: 8848)

## 🛠️ 配置说明

### 端口配置
```yaml
server:
  port: 9001  # 网关端口
```

### 服务发现
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 154.219.109.125:8848
        namespace: public
```

### 限流配置
```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 100  # 每秒补充令牌数
            redis-rate-limiter.burstCapacity: 200  # 桶容量
```

### 服务间认证
```yaml
service:
  auth:
    secret: "IM-Gateway-secret-key-for-service-authentication-2025"
```

## 🚦 启动步骤

1. **启动依赖服务**
   ```bash
   # 启动 Redis
   redis-server
   
   # 启动 Nacos
   startup.cmd -m standalone
   ```

2. **启动 IM-message-server**
   ```bash
   cd IM-message-server
   mvn spring-boot:run
   ```

3. **启动 IM-Gateway**
   ```bash
   cd IM-Gateway
   mvn spring-boot:run
   ```

## 📡 API 接口

### 网关管理接口

#### 健康检查
```http
GET http://localhost:9001/gateway/health
```

响应示例：
```json
{
  "service": "IM-Gateway",
  "status": "UP",
  "timestamp": "2025-11-13T19:30:00",
  "redis": {
    "status": "UP",
    "message": "Redis connection successful"
  }
}
```

#### 统计信息
```http
GET http://localhost:9001/gateway/stats
```

### 代理接口

#### WebSocket 连接
```javascript
const ws = new WebSocket('ws://localhost:9001/ws/user1');
```

#### 聊天 API
```http
POST http://localhost:9001/api/chat/send
Content-Type: application/json

{
  "senderId": "user1",
  "receiverId": "user2",
  "channelType": "PRIVATE",
  "contentType": "TEXT",
  "payload": {
    "text": "Hello World"
  }
}
```

#### 内部 API（需要服务间认证）
```http
POST http://localhost:9001/internal/chat/send
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "senderId": "service1",
  "receiverId": "user1",
  "channelType": "PRIVATE",
  "contentType": "SYSTEM",
  "payload": {
    "text": "System notification"
  }
}
```

## 🔍 监控和日志

### 日志级别
```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty: INFO
    org.example.imgateway: DEBUG
```

### 关键日志
- WebSocket 连接监控
- 限流触发记录
- 服务间认证日志
- 路由转发日志

## 🛡️ 安全特性

### 限流保护
- 基于 IP 的请求限流
- 基于用户的请求限流
- WebSocket 连接数限制

### 服务间认证
- JWT Token 自动生成和验证
- 请求头自动添加认证信息
- 内部 API 访问控制

## 🔧 开发调试

### 本地开发
1. 修改配置文件中的服务地址为本地地址
2. 启动本地 Redis 和 Nacos
3. 使用 IDE 启动服务

### 日志调试
```yaml
logging:
  level:
    org.springframework.cloud.gateway: TRACE
    org.example.imgateway: TRACE
```

## 📈 性能优化

### 连接池配置
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 10000
        response-timeout: 30s
        pool:
          max-connections: 500
          max-idle-time: 30s
```

### Redis 连接优化
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

## 🚨 故障排除

### 常见问题

1. **WebSocket 连接失败**
   - 检查 IM-message-server 是否启动
   - 验证 Nacos 服务注册状态
   - 查看网关路由配置

2. **限流触发**
   - 检查 Redis 连接状态
   - 调整限流参数
   - 查看限流日志

3. **服务间认证失败**
   - 验证 JWT 密钥配置
   - 检查 Token 生成逻辑
   - 查看认证过滤器日志

### 健康检查
```bash
# 检查网关状态
curl http://localhost:9001/gateway/health

# 检查后端服务状态
curl http://localhost:9001/actuator/health
```

## 📝 更新日志

### v1.0.0 (2025-11-13)
- 初始版本发布
- 支持 WebSocket 和 HTTP API 路由
- 集成服务间认证
- 添加限流和监控功能
