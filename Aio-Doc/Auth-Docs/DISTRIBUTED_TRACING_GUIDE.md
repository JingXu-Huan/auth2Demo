# 🔍 分布式链路追踪配置指南

**技术栈**: Spring Cloud Sleuth + Zipkin  
**版本**: Spring Cloud 2021.x

---

## 🎯 概述

项目使用 **Spring Cloud Sleuth** 进行分布式链路追踪，并通过 **Zipkin** 进行可视化展示。

---

## 📦 依赖配置

### 1. 父 POM 依赖管理

在 `pom.xml` 中添加 Spring Cloud 依赖管理：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2021.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 各服务模块添加依赖

在每个微服务的 `pom.xml` 中添加：

```xml
<!-- Spring Cloud Sleuth for distributed tracing -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>

<!-- Zipkin Client for trace reporting -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

**需要添加的服务**:
- ✅ User-server
- ✅ Oauth2-auth-server
- ✅ Email-server
- ✅ Gateway

---

## ⚙️ 配置文件

### User-server 配置示例

`User-server/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: user-server
  
  # Sleuth 链路追踪配置
  sleuth:
    sampler:
      # 采样率：1.0 表示 100% 采样（生产环境建议 0.1）
      probability: 1.0
    web:
      # 跳过不需要追踪的路径
      skip-pattern: /actuator.*|/health.*
  
  # Zipkin 配置
  zipkin:
    # Zipkin 服务器地址
    base-url: http://154.219.109.125:9411
    # 发送方式：web（HTTP）或 kafka/rabbitmq
    sender:
      type: web
    # 启用 Zipkin
    enabled: true
```

### Oauth2-auth-server 配置

`Oauth2-auth-server/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: oauth2-auth-server
  
  sleuth:
    sampler:
      probability: 1.0
    web:
      skip-pattern: /actuator.*|/health.*
  
  zipkin:
    base-url: http://154.219.109.125:9411
    sender:
      type: web
    enabled: true
```

### Gateway 配置

`Gateway/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: gateway
  
  sleuth:
    sampler:
      probability: 1.0
    web:
      skip-pattern: /actuator.*|/health.*
  
  zipkin:
    base-url: http://154.219.109.125:9411
    sender:
      type: web
    enabled: true
```

---

## 🚀 Zipkin 服务器部署

### 方式 1: Docker 部署（推荐）

```bash
# 拉取 Zipkin 镜像
docker pull openzipkin/zipkin

# 运行 Zipkin
docker run -d \
  --name zipkin \
  -p 9411:9411 \
  openzipkin/zipkin
```

### 方式 2: Docker Compose

`docker-compose.yml`:

```yaml
version: '3.8'

services:
  zipkin:
    image: openzipkin/zipkin
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem  # 使用内存存储（测试环境）
      # 生产环境建议使用 MySQL 或 Elasticsearch
      # - STORAGE_TYPE=mysql
      # - MYSQL_HOST=mysql
      # - MYSQL_USER=zipkin
      # - MYSQL_PASS=zipkin
    restart: unless-stopped
```

启动：
```bash
docker-compose up -d
```

### 方式 3: JAR 包运行

```bash
# 下载 Zipkin JAR
curl -sSL https://zipkin.io/quickstart.sh | bash -s

# 运行
java -jar zipkin.jar
```

---

## 🔍 核心概念

### 1. Trace（追踪）

一次完整的请求调用链路。

**示例**：用户登录请求
```
Gateway → Oauth2-auth-server → User-server
```

### 2. Span（跨度）

一次服务调用，是 Trace 的基本单元。

**示例**：
- Span 1: Gateway 接收请求
- Span 2: Oauth2-auth-server 处理认证
- Span 3: User-server 查询用户信息

### 3. Trace ID

全局唯一的追踪 ID，贯穿整个调用链。

**格式**: `80f198ee56343ba8`

### 4. Span ID

单个服务调用的 ID。

**格式**: `80f198ee56343ba8`

### 5. Parent Span ID

父 Span 的 ID，用于构建调用关系。

---

## 📊 日志中的追踪信息

### 自动添加的追踪信息

Sleuth 会自动在日志中添加追踪信息：

```
2025-11-10 18:00:00.123 INFO [user-server,80f198ee56343ba8,80f198ee56343ba8] 12345 --- [nio-8082-exec-1] c.e.u.controller.UserController : 查询用户信息
```

**格式说明**:
```
[服务名, Trace ID, Span ID]
[user-server, 80f198ee56343ba8, 80f198ee56343ba8]
```

### 日志配置（Logback）

`logback-spring.xml`:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>
                %d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%X{traceId:-},%X{spanId:-}] %c{1} - %m%n
            </pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

---

## 🎨 Zipkin UI 使用

### 访问地址

```
http://154.219.109.125:9411
```

### 主要功能

#### 1. 查询追踪

**搜索条件**:
- Service Name: 服务名（如 user-server）
- Span Name: 操作名（如 GET /api/users）
- Tags: 标签（如 http.status_code=200）
- Duration: 持续时间

#### 2. 查看调用链

点击某个 Trace，可以看到：
- 完整的调用链路
- 每个服务的耗时
- 调用关系（父子关系）

**示例**:
```
Gateway (100ms)
  └─ Oauth2-auth-server (80ms)
      └─ User-server (50ms)
```

#### 3. 依赖关系图

查看服务之间的依赖关系：
```
Gateway → Oauth2-auth-server → User-server
        ↘ Email-server
```

---

## 🔧 高级配置

### 1. 自定义采样率

**开发环境**（100% 采样）:
```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0
```

**生产环境**（10% 采样）:
```yaml
spring:
  sleuth:
    sampler:
      probability: 0.1
```

### 2. 跳过特定路径

```yaml
spring:
  sleuth:
    web:
      skip-pattern: |
        /actuator.*|
        /health.*|
        /metrics.*|
        /swagger.*|
        /v3/api-docs.*
```

### 3. 自定义 Span 名称

```java
@Service
public class UserService {
    
    @Autowired
    private Tracer tracer;
    
    public User getUser(Long id) {
        // 创建自定义 Span
        Span span = tracer.nextSpan().name("getUserById");
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            // 添加标签
            span.tag("user.id", String.valueOf(id));
            
            // 业务逻辑
            User user = userMapper.selectById(id);
            
            span.tag("user.found", user != null ? "true" : "false");
            return user;
        } finally {
            span.end();
        }
    }
}
```

### 4. 添加自定义标签

```java
@RestController
public class UserController {
    
    @Autowired
    private Tracer tracer;
    
    @GetMapping("/api/users/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        // 获取当前 Span
        Span span = tracer.currentSpan();
        
        if (span != null) {
            // 添加自定义标签
            span.tag("user.id", String.valueOf(id));
            span.tag("business.type", "user-query");
        }
        
        User user = userService.getUser(id);
        return Result.success(user);
    }
}
```

### 5. 异步调用追踪

```java
@Service
public class AsyncService {
    
    @Async
    @NewSpan  // 创建新的 Span
    public void asyncTask() {
        // 异步任务会自动追踪
        log.info("执行异步任务");
    }
}
```

---

## 🔗 Feign 调用追踪

Sleuth 会自动追踪 Feign 调用，无需额外配置。

```java
@FeignClient(name = "user-server")
public interface UserServiceClient {
    
    @GetMapping("/api/users/{id}")
    Result<User> getUser(@PathVariable Long id);
}
```

**调用链**:
```
Oauth2-auth-server (调用方)
  └─ HTTP GET /api/users/1
      └─ User-server (被调用方)
```

---

## 📈 性能优化

### 1. 使用异步发送

```yaml
spring:
  zipkin:
    sender:
      type: web
    # 使用异步发送，不阻塞主线程
    message-timeout: 1
```

### 2. 使用 Kafka/RabbitMQ

**高吞吐量场景**，建议使用消息队列：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

```yaml
spring:
  zipkin:
    sender:
      type: kafka
  kafka:
    bootstrap-servers: localhost:9092
```

### 3. Zipkin 持久化存储

**生产环境建议使用 MySQL 或 Elasticsearch**：

```bash
docker run -d \
  --name zipkin \
  -p 9411:9411 \
  -e STORAGE_TYPE=mysql \
  -e MYSQL_HOST=mysql \
  -e MYSQL_USER=zipkin \
  -e MYSQL_PASS=zipkin \
  openzipkin/zipkin
```

---

## 🎯 实际应用场景

### 1. 登录流程追踪

**请求路径**:
```
POST /oauth/token
  ↓
Gateway (转发)
  ↓
Oauth2-auth-server (认证)
  ↓
User-server (查询用户)
```

**Zipkin 显示**:
```
Trace ID: 80f198ee56343ba8
├─ gateway (50ms)
│   └─ oauth2-auth-server (200ms)
│       └─ user-server (100ms)
└─ Total: 350ms
```

### 2. 用户注册追踪

**请求路径**:
```
POST /api/users/register
  ↓
Gateway
  ↓
User-server (创建用户)
  ↓
Email-server (发送邮件)
```

### 3. 性能瓶颈分析

通过 Zipkin 可以快速定位：
- 哪个服务响应慢
- 哪个接口耗时长
- 数据库查询是否慢

---

## 🚨 常见问题

### 1. Zipkin 连接失败

**问题**: 服务启动报错 `Connection refused: zipkin`

**解决**:
```yaml
spring:
  zipkin:
    enabled: false  # 临时禁用
```

或确保 Zipkin 服务已启动：
```bash
docker ps | grep zipkin
```

### 2. 日志中没有 Trace ID

**问题**: 日志格式不正确

**解决**: 检查 Logback 配置
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-},%X{spanId:-}] %m%n</pattern>
```

### 3. 采样率过低

**问题**: Zipkin 中看不到追踪数据

**解决**: 提高采样率
```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0  # 100% 采样
```

---

## ✅ 配置检查清单

- [ ] 添加 Sleuth 和 Zipkin 依赖
- [ ] 配置 `spring.application.name`
- [ ] 配置 Zipkin 服务器地址
- [ ] 设置采样率
- [ ] 启动 Zipkin 服务器
- [ ] 测试追踪功能
- [ ] 查看 Zipkin UI

---

## 📊 完整配置示例

### application.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: user-server
  
  # 链路追踪配置
  sleuth:
    sampler:
      probability: 1.0  # 100% 采样
    web:
      skip-pattern: /actuator.*|/health.*
  
  zipkin:
    base-url: http://154.219.109.125:9411
    sender:
      type: web
    enabled: true
```

---

## 🎉 总结

### 已配置的服务

- ✅ User-server (8082)
- ✅ Oauth2-auth-server (8080)
- ✅ Gateway (9000)
- ✅ Email-server

### Zipkin 地址

```
http://154.219.109.125:9411
```

### 核心特性

1. ✅ **自动追踪** - 无需手动编码
2. ✅ **Feign 支持** - 自动追踪微服务调用
3. ✅ **日志集成** - 自动添加 Trace ID
4. ✅ **可视化** - Zipkin UI 展示
5. ✅ **性能分析** - 定位瓶颈

---

**链路追踪已完整配置！** 🎊

现在每个请求都会自动生成追踪信息，可以在 Zipkin UI 中查看完整的调用链路！
