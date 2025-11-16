# IM Message Server

基于 Spring Boot 的即时消息服务器，支持实时聊天功能。

## 功能特性

- **WebSocket 实时通信**: 支持实时双向通信
- **Redis 分布式消息**: 使用 Redis 发布/订阅模式实现分布式消息传递
- **单聊和群聊**: 支持一对一聊天和群组聊天
- **Fan-out 群聊**: 采用扇出模式高效处理群聊消息
- **REST API**: 提供 HTTP 接口用于消息发送和状态查询
- **异常处理**: 完善的异常处理和错误日志记录

## 技术栈

- Spring Boot 2.x
- Spring WebSocket
- Redis (发布/订阅)
- Lombok
- Gson
- Maven

## 快速开始

### 环境要求

- JDK 17+
- Redis 服务器
- Maven 3.6+

### 启动步骤

1. **启动 Redis 服务器**
   ```bash
   redis-server
   ```

2. **配置应用**
   
   修改 `application.yml` 中的 Redis 连接配置：
   ```yaml
   spring:
     redis:
       host: localhost
       port: 6379
   ```

3. **启动应用**
   ```bash
   mvn spring-boot:run
   ```

4. **验证服务**
   
   访问健康检查接口：
   ```
   GET http://localhost:8002/api/chat/health
   ```

## API 接口

### WebSocket 连接

连接地址：`ws://localhost:8002/ws/{userId}`

示例：`ws://localhost:8002/ws/user123`

### REST API

#### 发送消息
```
POST /api/chat/send
Content-Type: application/json

{
    "senderId": "user1",
    "receiverId": "user2", 
    "content": "Hello!",
    "type": "PRIVATE"
}
```

#### 健康检查
```
GET /api/chat/health
```

#### 在线用户统计
```
GET /api/chat/online-users
```

## 消息格式

### WebSocket 消息格式

```json
{
    "senderId": "user1",
    "receiverId": "user2",
    "content": "消息内容",
    "type": "PRIVATE",
    "groupId": null,
    "timestamp": 1699876543210
}
```

### 消息类型

- `PRIVATE`: 单聊消息
- `GROUP`: 群聊消息

## 架构设计

### 核心组件

1. **MyWebSocketHandler**: WebSocket 连接处理器
2. **ChatService**: 消息处理业务逻辑
3. **WebSocketSessionManager**: 本地 WebSocket 会话管理
4. **RedisMessagePublisher**: Redis 消息发布器
5. **RedisMessageSubscriber**: Redis 消息订阅器

### 消息流程

1. 客户端通过 WebSocket 发送消息
2. `MyWebSocketHandler` 接收并解析消息
3. `ChatService` 处理消息逻辑
4. 单聊消息直接发布到 Redis
5. 群聊消息通过 Fan-out 模式转换为多个单聊消息
6. `RedisMessageSubscriber` 订阅消息并推送给目标用户

## 配置说明

### application.yml 配置项

```yaml
server:
  port: 8002                    # 服务端口

spring:
  redis:
    host: localhost             # Redis 主机
    port: 6379                  # Redis 端口
    database: 0                 # Redis 数据库

chat:
  redis:
    topic: chat-messages        # Redis 消息主题

logging:
  level:
    org.example.imserver: INFO  # 日志级别
```

## 开发指南

### 扩展功能

1. **消息持久化**: 实现 `MessageRepository` 接口
2. **用户认证**: 添加 JWT 或 OAuth2 认证
3. **群组管理**: 实现 `GroupService` 接口
4. **消息状态**: 添加已读/未读状态管理
5. **文件传输**: 支持图片、文件等多媒体消息

### 部署建议

1. **生产环境**: 使用 Redis 集群提高可用性
2. **负载均衡**: 部署多个实例并使用负载均衡器
3. **监控**: 添加应用监控和日志收集
4. **安全**: 启用 HTTPS 和 WebSocket 安全连接

## 故障排除

### 常见问题

1. **WebSocket 连接失败**
   - 检查用户ID是否正确传递
   - 确认端口8002未被占用

2. **Redis 连接失败**
   - 确认 Redis 服务正在运行
   - 检查连接配置是否正确

3. **消息发送失败**
   - 查看应用日志获取详细错误信息
   - 确认消息格式是否正确

## 许可证

本项目采用 MIT 许可证。