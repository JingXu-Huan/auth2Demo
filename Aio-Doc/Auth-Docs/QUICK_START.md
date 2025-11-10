# 🚀 快速开始指南

## 5 分钟快速体验

### 前置准备

确保已安装：
- ✅ JDK 1.8+
- ✅ Maven 3.6+
- ✅ PostgreSQL 客户端（可选）

### 步骤 1：克隆项目

```bash
cd G:\Projects\Java_Study\test\01\auth2Demo
```

### 步骤 2：初始化数据库

```bash
# 连接到 PostgreSQL
psql -h 101.42.157.163 -p 5432 -U user -d aio

# 执行建表语句
\i database/schema_postgresql.sql

# 验证表是否创建成功
\dt
```

### 步骤 3：编译项目

```bash
mvn clean install -DskipTests
```

### 步骤 4：启动服务

**按顺序启动以下服务：**

```bash
# 1. 启动 OAuth2 认证服务 (8080)
cd Oauth2-auth-server
mvn spring-boot:run

# 2. 启动用户服务 (8082)
cd User-server
mvn spring-boot:run

# 3. 启动邮件服务 (8083)
cd Email-server
mvn spring-boot:run

# 4. 启动网关 (9000)
cd Gateway
mvn spring-boot:run
```

### 步骤 5：测试接口

#### 5.1 用户注册

```bash
curl -X POST http://localhost:9000/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@123"
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "testuser",
    "email": "test@example.com"
  }
}
```

#### 5.2 发送验证码

```bash
curl -X POST http://localhost:9000/api/email/send-code \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'
```

#### 5.3 验证邮箱

```bash
curl -X POST http://localhost:9000/api/email/verify-and-activate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "code": "123456"
  }'
```

#### 5.4 用户登录

```bash
curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=test@example.com&password=Test@123&client_id=client&client_secret=secret"
```

**预期响应：**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 7199,
  "scope": "read write"
}
```

#### 5.5 访问受保护资源

```bash
curl -X GET http://localhost:9000/api/users/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 完整流程演示

### 场景 1：新用户注册并登录

```bash
# 1. 注册
curl -X POST http://localhost:9000/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "Alice@123"
  }'

# 2. 发送验证码
curl -X POST http://localhost:9000/api/email/send-code \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com"}'

# 3. 验证邮箱（假设验证码是 123456）
curl -X POST http://localhost:9000/api/email/verify-and-activate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "code": "123456"
  }'

# 4. 登录
curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=alice@example.com&password=Alice@123&client_id=client&client_secret=secret"
```

### 场景 2：长时间未登录用户登录

```bash
# 1. 检查是否需要安全验证
curl -X GET "http://localhost:9000/api/security/check?email=alice@example.com"

# 2. 如果需要验证，发送安全验证码
curl -X POST http://localhost:9000/api/security/send-code \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com"}'

# 3. 验证安全验证码
curl -X POST http://localhost:9000/api/security/verify-code \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "code": "123456"
  }'

# 4. 登录
curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=alice@example.com&password=Alice@123&client_id=client&client_secret=secret"
```

### 场景 3：修改密码

```bash
# 1. 先登录获取 Token
TOKEN=$(curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=alice@example.com&password=Alice@123&client_id=client&client_secret=secret" \
  | jq -r '.access_token')

# 2. 修改密码
curl -X POST http://localhost:9000/api/users/1/change-password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "Alice@123",
    "newPassword": "NewAlice@456"
  }'
```

---

## 使用 Postman 测试

### 导入 Postman Collection

1. 打开 Postman
2. 点击 Import
3. 导入文件：`Aio-Doc/Auth-Docs/postman_collection.json`

### 环境变量配置

```json
{
  "base_url": "http://localhost:9000",
  "client_id": "client",
  "client_secret": "secret",
  "access_token": ""
}
```

### 测试步骤

1. **用户注册** → 保存 userId
2. **发送验证码** → 查看邮箱
3. **验证邮箱** → 激活账户
4. **用户登录** → 保存 access_token
5. **获取用户信息** → 使用 access_token

---

## 使用 Swagger UI 测试

### 访问地址

- **Gateway**: http://localhost:9000/doc.html
- **OAuth2-Auth**: http://localhost:8080/doc.html
- **User-Server**: http://localhost:8082/doc.html
- **Email-Server**: http://localhost:8083/doc.html

### 使用步骤

1. 打开 Swagger UI
2. 找到对应的接口
3. 点击 "Try it out"
4. 填写参数
5. 点击 "Execute"
6. 查看响应

---

## 常见问题排查

### Q1: 服务启动失败

**检查步骤：**

```bash
# 1. 检查端口占用
netstat -ano | findstr "8080"
netstat -ano | findstr "8082"
netstat -ano | findstr "9000"

# 2. 检查数据库连接
psql -h 101.42.157.163 -p 5432 -U user -d aio -c "SELECT 1"

# 3. 检查 Redis 连接
redis-cli -h localhost -p 6379 ping

# 4. 查看日志
tail -f logs/spring.log
```

### Q2: 登录失败

**可能原因：**

1. 邮箱未验证
   ```bash
   # 解决：验证邮箱
   curl -X POST http://localhost:9000/api/email/verify-and-activate \
     -H "Content-Type: application/json" \
     -d '{"email": "test@example.com", "code": "123456"}'
   ```

2. 密码错误
   ```bash
   # 检查密码是否符合强密码要求
   # 至少8位，包含大小写字母、数字和特殊字符
   ```

3. 账户被锁定
   ```bash
   # 等待 15 分钟自动解锁
   ```

### Q3: Token 无效

**检查步骤：**

```bash
# 1. 检查 Token 是否过期
# Access Token 有效期：2 小时

# 2. 刷新 Token
curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&refresh_token=YOUR_REFRESH_TOKEN&client_id=client&client_secret=secret"
```

### Q4: 验证码收不到

**检查步骤：**

```bash
# 1. 检查 RabbitMQ 是否运行
# 2. 检查邮件服务配置
# 3. 查看 Email-server 日志
```

---

## 开发环境配置

### IDE 配置（IntelliJ IDEA）

1. **导入项目**
   - File → Open → 选择项目根目录
   - 等待 Maven 依赖下载完成

2. **配置 JDK**
   - File → Project Structure → Project
   - 设置 Project SDK 为 JDK 1.8

3. **配置运行配置**
   - Run → Edit Configurations
   - 添加 Spring Boot 配置
   - 设置 Main class 和 Module

4. **启用热部署**
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-devtools</artifactId>
       <optional>true</optional>
   </dependency>
   ```

### 数据库工具

推荐使用：
- **DBeaver** (免费)
- **DataGrip** (付费)
- **pgAdmin** (PostgreSQL 官方)

连接配置：
```
Host: 101.42.157.163
Port: 5432
Database: aio
Username: user
Password: 202430904JINGxu
```

---

## 下一步

- 📖 阅读 [API 文档](./API_REFERENCE.md)
- 🔒 了解 [安全机制](./SECURITY_DESIGN.md)
- 🗄️ 查看 [数据库设计](./DATABASE_DESIGN.md)
- 🚀 学习 [部署指南](./DEPLOYMENT.md)

---

## 获取帮助

- 📚 查看完整文档：`Aio-Doc/Auth-Docs/`
- 🐛 报告问题：创建 Issue
- 💬 技术讨论：联系开发团队

---

**祝您使用愉快！** 🎉
