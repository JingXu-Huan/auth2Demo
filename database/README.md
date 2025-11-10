# 数据库初始化脚本

## 📋 脚本说明

本目录包含所有微服务的数据库建表语句，**使用现有的 aio 数据库**，整合了原有的 OAuth2 认证系统设计。

## 📂 文件列表

| 文件名 | 说明 | 表数量 |
|--------|------|--------|
| `01_user_service.sql` | 用户服务（整合版） | 7个表 |
| `02_message_service.sql` | 消息服务 | 3个表 |
| `03_group_service.sql` | 群组服务 | 2个表 |
| `init_all.sql` | 一键初始化所有表 | - |
| `schema_postgresql.sql` | 原始设计（已整合） | - |

## 🎯 重要说明

### 数据库信息
- **数据库名**: `aio` （使用现有数据库，不创建新数据库）
- **主机**: 101.42.157.163
- **端口**: 5432
- **用户**: user
- **密码**: 202430904JINGxu

### 设计特点
1. ✅ 整合了原有的 OAuth2 认证系统设计
2. ✅ 支持多种登录方式（email/gitee/github）
3. ✅ 完整的账户状态管理
4. ✅ 登录日志审计
5. ✅ 密码历史防重复
6. ✅ 设备管理
7. ✅ 好友关系
8. ✅ 邮箱验证

## 🚀 使用方法

### 方法1: 远程服务器执行（推荐）

```bash
# 连接到远程数据库并执行
psql -U user -d aio -h 101.42.157.163 -f 01_user_service.sql
psql -U user -d aio -h 101.42.157.163 -f 02_message_service.sql
psql -U user -d aio -h 101.42.157.163 -f 03_group_service.sql
```

### 方法2: 一键初始化

```bash
# 远程服务器
psql -U user -d aio -h 101.42.157.163 -f init_all.sql

# 本地数据库
psql -U postgres -d aio -f init_all.sql
```

### 方法3: 交互式执行

```bash
# 先连接数据库
psql -U user -d aio -h 101.42.157.163

# 然后在 psql 中执行
\i 01_user_service.sql
\i 02_message_service.sql
\i 03_group_service.sql
```

## 📊 数据库结构

### 用户服务 (aio_user)
- `users` - 用户表
- `user_friends` - 好友关系表
- `password_history` - 密码历史表
- `login_logs` - 登录日志表
- `user_devices` - 设备管理表
- `email_verification_codes` - 邮箱验证码表

### 消息服务 (aio_message)
- `messages` - 消息表
- `message_read_status` - 消息已读状态表
- `conversations` - 会话表

### 群组服务 (aio_group)
- `groups` - 群组表
- `group_members` - 群组成员表

## ⚠️ 注意事项

1. **执行顺序**: 请按文件编号顺序执行
2. **权限要求**: 需要 PostgreSQL 超级用户权限
3. **数据库版本**: PostgreSQL 13+
4. **字符编码**: UTF-8
5. **备份**: 执行前请备份现有数据

## 🔧 常用命令

### 查看所有数据库
```sql
\l
```

### 切换数据库
```sql
\c aio_user
```

### 查看所有表
```sql
\dt
```

### 查看表结构
```sql
\d users
```

### 删除数据库（谨慎使用）
```sql
DROP DATABASE IF EXISTS aio_user;
```

## 📝 更新日志

- **v1.0.0** (2025-11-11): 初始版本
  - 用户服务建表语句
  - 消息服务建表语句
  - 群组服务建表语句

---

**维护人**: 数据库团队  
**最后更新**: 2025-11-11
