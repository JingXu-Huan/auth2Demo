# 📚 OAuth2 认证系统文档索引

欢迎使用 OAuth2 认证系统！本文档将帮助您快速了解和使用系统。

---

## 📖 文档导航

### 🚀 新手入门

| 文档 | 说明 | 适合人群 |
|------|------|---------|
| [README](./README.md) | 系统概述和架构介绍 | 所有人 |
| [快速开始](./QUICK_START.md) | 5分钟快速体验 | 开发者 |
| [API 参考](./API_REFERENCE.md) | 完整 API 接口文档 | 前端/后端开发者 |

### 🔧 技术文档

| 文档 | 说明 | 适合人群 |
|------|------|---------|
| [数据库设计](./DATABASE_DESIGN.md) | 数据库表结构和设计说明 | 后端开发者/DBA |
| [安全设计](../SECURITY_IMPROVEMENTS.md) | 安全机制和改进说明 | 安全工程师 |
| [数据库迁移](../DATABASE_MIGRATION_GUIDE.md) | MySQL → PostgreSQL 迁移指南 | 运维工程师 |

### 📦 部署运维

| 文档 | 说明 | 适合人群 |
|------|------|---------|
| 部署指南 | 生产环境部署步骤 | 运维工程师 |
| 监控配置 | 系统监控和告警配置 | 运维工程师 |
| 故障排查 | 常见问题和解决方案 | 所有人 |

---

## 🎯 按角色查看

### 👨‍💻 前端开发者

**推荐阅读顺序：**
1. [README](./README.md) - 了解系统架构
2. [API 参考](./API_REFERENCE.md) - 查看接口文档
3. [快速开始](./QUICK_START.md) - 本地测试接口

**关键信息：**
- 网关地址：`http://localhost:9000`
- Swagger 文档：`http://localhost:9000/doc.html`
- Token 有效期：2 小时
- 刷新 Token 有效期：7 天

### 👨‍💻 后端开发者

**推荐阅读顺序：**
1. [README](./README.md) - 了解系统架构
2. [数据库设计](./DATABASE_DESIGN.md) - 理解数据模型
3. [快速开始](./QUICK_START.md) - 搭建开发环境
4. [API 参考](./API_REFERENCE.md) - 实现业务逻辑

**关键信息：**
- 技术栈：Spring Boot 2.7.18 + Spring Cloud 2021.0.8
- 数据库：PostgreSQL 12+
- 缓存：Redis
- 消息队列：RabbitMQ

### 🔒 安全工程师

**推荐阅读顺序：**
1. [安全设计](../SECURITY_IMPROVEMENTS.md) - 了解安全机制
2. [数据库设计](./DATABASE_DESIGN.md) - 查看敏感数据存储
3. [API 参考](./API_REFERENCE.md) - 审查接口安全

**关键信息：**
- 密码加密：BCrypt (强度 10)
- 登录限制：5 次失败锁定 15 分钟
- Token 加密：JWT
- 敏感数据：@JsonIgnore 注解

### 🛠️ 运维工程师

**推荐阅读顺序：**
1. [README](./README.md) - 了解系统架构
2. [数据库迁移](../DATABASE_MIGRATION_GUIDE.md) - 数据库部署
3. 部署指南 - 服务部署
4. 监控配置 - 系统监控

**关键信息：**
- 服务端口：8080, 8082, 8083, 9000
- 数据库：101.42.157.163:5432
- 监控：Prometheus + Grafana
- 链路追踪：Zipkin

---

## 🔍 按场景查看

### 场景 1：新项目接入

1. 阅读 [README](./README.md) 了解系统
2. 参考 [快速开始](./QUICK_START.md) 搭建环境
3. 查看 [API 参考](./API_REFERENCE.md) 集成接口
4. 测试完整流程

### 场景 2：功能开发

1. 查看 [数据库设计](./DATABASE_DESIGN.md) 了解数据模型
2. 参考现有代码实现
3. 编写单元测试
4. 更新 API 文档

### 场景 3：问题排查

1. 查看 [快速开始](./QUICK_START.md) 的常见问题
2. 检查服务日志
3. 验证数据库连接
4. 测试 API 接口

### 场景 4：系统部署

1. 阅读 [数据库迁移](../DATABASE_MIGRATION_GUIDE.md)
2. 执行数据库初始化
3. 配置环境变量
4. 启动服务并验证

---

## 📊 系统概览

### 核心功能

- ✅ 用户注册
- ✅ 邮箱验证
- ✅ 用户登录
- ✅ Token 刷新
- ✅ 密码修改
- ✅ 第三方登录（Gitee）
- ✅ 登录失败限制
- ✅ 长时间未登录验证
- ✅ 强密码验证

### 技术特性

- ✅ 微服务架构
- ✅ OAuth2 认证
- ✅ JWT Token
- ✅ Redis 缓存
- ✅ RabbitMQ 消息队列
- ✅ Sentinel 流量控制
- ✅ Zipkin 链路追踪
- ✅ Swagger API 文档

### 安全特性

- ✅ BCrypt 密码加密
- ✅ 强密码策略
- ✅ 登录失败限制
- ✅ 账户锁定机制
- ✅ 密码历史记录
- ✅ 登录日志审计
- ✅ Token 过期管理
- ✅ 服务间认证

---

## 🔗 快速链接

### 在线资源

- **Swagger UI**: http://localhost:9000/doc.html
- **Zipkin**: http://154.219.109.125:9411
- **Sentinel**: http://localhost:8858

### 本地资源

- **项目根目录**: `G:\Projects\Java_Study\test\01\auth2Demo`
- **文档目录**: `G:\Projects\Java_Study\test\01\auth2Demo\Aio-Doc\Auth-Docs`
- **数据库脚本**: `database/schema_postgresql.sql`

### 配置文件

- **OAuth2-Auth**: `Oauth2-auth-server/src/main/resources/application.yml`
- **User-Server**: `User-server/src/main/resources/application.yml`
- **Email-Server**: `Email-server/src/main/resources/application.yml`
- **Gateway**: `Gateway/src/main/resources/application.yml`

---

## 📝 文档更新记录

| 日期 | 版本 | 更新内容 |
|------|------|---------|
| 2025-11-10 | v2.0 | 完整文档体系建立 |
| 2025-11-10 | v2.0 | 数据库迁移到 PostgreSQL |
| 2025-11-10 | v2.0 | 添加强密码验证 |
| 2025-11-10 | v2.0 | 完善安全机制 |

---

## 💡 贡献指南

### 文档贡献

欢迎贡献文档！请遵循以下规范：

1. **Markdown 格式**
   - 使用标准 Markdown 语法
   - 代码块指定语言
   - 表格对齐

2. **文档结构**
   - 清晰的标题层级
   - 完整的目录
   - 适当的示例

3. **内容要求**
   - 准确性
   - 完整性
   - 易读性

### 代码贡献

1. Fork 项目
2. 创建特性分支
3. 提交代码
4. 创建 Pull Request

---

## 📞 联系方式

- **项目位置**: G:\Projects\Java_Study\test\01\auth2Demo
- **文档位置**: G:\Projects\Java_Study\test\01\auth2Demo\Aio-Doc\Auth-Docs
- **更新日期**: 2025-11-10
- **版本**: 2.0

---

## 🎓 学习路径

### 初级（1-2 天）

1. 了解系统架构
2. 搭建开发环境
3. 测试基本接口
4. 理解认证流程

### 中级（3-5 天）

1. 深入理解 OAuth2
2. 掌握数据库设计
3. 实现业务功能
4. 编写单元测试

### 高级（1-2 周）

1. 性能优化
2. 安全加固
3. 监控告警
4. 故障排查

---

## 🎯 下一步行动

### 新手

1. ✅ 阅读 [README](./README.md)
2. ✅ 完成 [快速开始](./QUICK_START.md)
3. ✅ 测试 API 接口

### 开发者

1. ✅ 搭建开发环境
2. ✅ 理解代码结构
3. ✅ 开始功能开发

### 运维

1. ✅ 准备部署环境
2. ✅ 初始化数据库
3. ✅ 部署服务
4. ✅ 配置监控

---

**开始您的旅程吧！** 🚀

如有任何问题，请查阅相关文档或联系开发团队。
