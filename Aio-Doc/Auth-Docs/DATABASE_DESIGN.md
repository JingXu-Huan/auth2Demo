# 🗄️ 数据库设计文档

## 数据库信息

- **类型**: PostgreSQL 12+
- **主机**: 101.42.157.163
- **端口**: 5432
- **数据库**: aio
- **用户**: user
- **Schema**: public

---

## 表结构设计

### 1. users (用户主表)

存储用户的基本信息和账户状态。

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    email_verified BOOLEAN DEFAULT FALSE,
    avatar_url VARCHAR(500),
    
    -- 邮箱验证相关
    confirmation_token VARCHAR(255),
    token_expiry TIMESTAMP,
    
    -- 登录相关
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(50),
    
    -- 账户状态
    enabled BOOLEAN DEFAULT TRUE,
    account_non_expired BOOLEAN DEFAULT TRUE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    credentials_non_expired BOOLEAN DEFAULT TRUE,
    lock_reason VARCHAR(255),
    locked_at TIMESTAMP,
    
    -- 时间戳
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| display_name | VARCHAR(100) | 显示名称 |
| email | VARCHAR(255) | 邮箱，唯一 |
| email_verified | BOOLEAN | 邮箱是否已验证 |
| avatar_url | VARCHAR(500) | 头像URL |
| confirmation_token | VARCHAR(255) | 邮箱验证令牌 |
| token_expiry | TIMESTAMP | 令牌过期时间 |
| last_login_at | TIMESTAMP | 最后登录时间 |
| last_login_ip | VARCHAR(50) | 最后登录IP |
| enabled | BOOLEAN | 账户是否启用 |
| account_non_expired | BOOLEAN | 账户是否未过期 |
| account_non_locked | BOOLEAN | 账户是否未锁定 |
| credentials_non_expired | BOOLEAN | 密码是否未过期 |
| lock_reason | VARCHAR(255) | 锁定原因 |
| locked_at | TIMESTAMP | 锁定时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 索引

```sql
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_users_last_login ON users(last_login_at);
```

---

### 2. user_credentials (用户凭证表)

支持多种登录方式（邮箱密码、第三方登录）。

```sql
CREATE TABLE user_credentials (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255),
    password_hash VARCHAR(255),
    access_token TEXT,
    refresh_token TEXT,
    token_expiry TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_provider UNIQUE (user_id, provider),
    CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键，自增 |
| user_id | BIGINT | 用户ID，外键 |
| provider | VARCHAR(50) | 认证提供商（email/gitee/github） |
| provider_user_id | VARCHAR(255) | 第三方平台用户ID |
| password_hash | VARCHAR(255) | 密码哈希（仅email登录） |
| access_token | TEXT | 第三方访问令牌 |
| refresh_token | TEXT | 第三方刷新令牌 |
| token_expiry | TIMESTAMP | 令牌过期时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### Provider 类型

- `email`: 邮箱密码登录
- `gitee`: Gitee 第三方登录
- `github`: GitHub 第三方登录
- `wechat`: 微信登录
- `qq`: QQ 登录

#### 索引

```sql
CREATE INDEX idx_user_credentials_provider_user 
    ON user_credentials(provider, provider_user_id);
```

---

### 3. password_history (密码历史表)

记录用户的历史密码，防止重复使用。

```sql
CREATE TABLE password_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键，自增 |
| user_id | BIGINT | 用户ID，外键 |
| password_hash | VARCHAR(255) | 密码哈希 |
| created_at | TIMESTAMP | 创建时间 |

#### 索引

```sql
CREATE INDEX idx_password_history_user_id ON password_history(user_id);
CREATE INDEX idx_password_history_created_at ON password_history(created_at);
```

#### 使用说明

- 保留最近 5 次密码记录
- 修改密码时检查新密码是否在历史记录中
- 定期清理过期记录（超过 1 年）

---

### 4. login_logs (登录日志表)

记录所有登录尝试，用于安全审计。

```sql
CREATE TABLE login_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(255),
    ip_address VARCHAR(50) NOT NULL,
    user_agent TEXT,
    login_status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(255),
    location VARCHAR(255),
    device_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_login_logs_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE SET NULL
);
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键，自增 |
| user_id | BIGINT | 用户ID，外键（可为空） |
| email | VARCHAR(255) | 登录邮箱 |
| ip_address | VARCHAR(50) | IP地址 |
| user_agent | TEXT | 用户代理（浏览器信息） |
| login_status | VARCHAR(20) | 登录状态 |
| failure_reason | VARCHAR(255) | 失败原因 |
| location | VARCHAR(255) | 登录地点（可选） |
| device_type | VARCHAR(50) | 设备类型 |
| created_at | TIMESTAMP | 创建时间 |

#### Login Status 类型

- `SUCCESS`: 登录成功
- `FAILED`: 登录失败
- `BLOCKED`: 账户被锁定

#### 索引

```sql
CREATE INDEX idx_login_logs_user_id ON login_logs(user_id);
CREATE INDEX idx_login_logs_email ON login_logs(email);
CREATE INDEX idx_login_logs_ip_address ON login_logs(ip_address);
CREATE INDEX idx_login_logs_login_status ON login_logs(login_status);
CREATE INDEX idx_login_logs_created_at ON login_logs(created_at);
```

---

## ER 图

```
┌─────────────────────┐
│       users         │
│─────────────────────│
│ id (PK)             │
│ username            │
│ email               │
│ email_verified      │
│ enabled             │
│ last_login_at       │
│ ...                 │
└──────────┬──────────┘
           │
           │ 1:N
           │
    ┌──────┴──────┬──────────────┬──────────────┐
    │             │              │              │
    ↓             ↓              ↓              ↓
┌───────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│user_      │ │password_ │ │login_    │ │oauth_    │
│credentials│ │history   │ │logs      │ │tokens    │
│───────────│ │──────────│ │──────────│ │──────────│
│id (PK)    │ │id (PK)   │ │id (PK)   │ │id (PK)   │
│user_id(FK)│ │user_id   │ │user_id   │ │user_id   │
│provider   │ │password_ │ │email     │ │token     │
│password_  │ │hash      │ │ip_address│ │...       │
│hash       │ │...       │ │status    │ │          │
│...        │ │          │ │...       │ │          │
└───────────┘ └──────────┘ └──────────┘ └──────────┘
```

---

## 数据字典

### 账户状态说明

| 字段 | 值 | 说明 |
|------|---|------|
| enabled | true | 账户启用 |
| enabled | false | 账户禁用（管理员操作） |
| account_non_expired | true | 账户未过期 |
| account_non_expired | false | 账户已过期 |
| account_non_locked | true | 账户未锁定 |
| account_non_locked | false | 账户已锁定（管理员手动锁定） |
| credentials_non_expired | true | 密码未过期 |
| credentials_non_expired | false | 密码已过期（需要修改密码） |

### 登录状态说明

| 状态 | 说明 | 处理方式 |
|------|------|---------|
| SUCCESS | 登录成功 | 更新 last_login_at |
| FAILED | 登录失败 | 记录失败次数 |
| BLOCKED | 账户锁定 | 拒绝登录 |

---

## 数据迁移

### 从 MySQL 迁移到 PostgreSQL

主要差异：

| MySQL | PostgreSQL |
|-------|-----------|
| `AUTO_INCREMENT` | `SERIAL` / `BIGSERIAL` |
| `DATETIME` | `TIMESTAMP` |
| `TINYINT(1)` | `BOOLEAN` |
| `ON UPDATE CURRENT_TIMESTAMP` | 触发器 |

### 迁移步骤

1. **导出 MySQL 数据**
```bash
mysqldump -h host -u user -p database > mysql_data.sql
```

2. **转换 SQL 语法**
- 使用工具或手动转换

3. **导入 PostgreSQL**
```bash
psql -h 101.42.157.163 -U user -d aio -f converted_data.sql
```

---

## 性能优化

### 索引策略

1. **主键索引**: 自动创建
2. **唯一索引**: email, username
3. **查询索引**: 
   - users(email, enabled)
   - login_logs(created_at, login_status)
   - password_history(user_id, created_at)

### 分区策略

对于大表（如 login_logs），可以按时间分区：

```sql
-- 按月分区
CREATE TABLE login_logs_2025_11 PARTITION OF login_logs
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE login_logs_2025_12 PARTITION OF login_logs
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');
```

### 清理策略

```sql
-- 清理 90 天前的登录日志
DELETE FROM login_logs 
WHERE created_at < NOW() - INTERVAL '90 days';

-- 清理 1 年前的密码历史
DELETE FROM password_history 
WHERE created_at < NOW() - INTERVAL '1 year';
```

---

## 备份策略

### 全量备份

```bash
# 每天凌晨 2 点执行
pg_dump -h 101.42.157.163 -U user -d aio -F c -f backup_$(date +%Y%m%d).dump
```

### 增量备份

```bash
# 使用 WAL 归档
archive_mode = on
archive_command = 'cp %p /backup/wal/%f'
```

### 恢复

```bash
# 从备份恢复
pg_restore -h 101.42.157.163 -U user -d aio backup_20251110.dump
```

---

## 安全建议

1. **密码存储**: 使用 BCrypt，强度 10
2. **敏感字段**: 使用 `@JsonIgnore` 注解
3. **SQL 注入**: 使用 MyBatis 参数化查询
4. **权限控制**: 最小权限原则
5. **数据加密**: 敏感数据加密存储

---

## 监控指标

### 关键指标

- 表大小
- 索引使用率
- 慢查询
- 连接数
- 缓存命中率

### 监控 SQL

```sql
-- 查看表大小
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 查看索引使用情况
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- 查看慢查询
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    max_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

---

## 更新日志

- **2025-11-10**: 初始版本
- 从 MySQL 迁移到 PostgreSQL
- 添加密码历史表
- 添加登录日志表
- 完善索引和约束
