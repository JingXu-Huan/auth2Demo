# 🏗️ 企业级协作平台 - 完整数据库架构设计

## 📋 概述

本数据库架构基于 **PostgreSQL 14+**，采用**微服务数据库隔离**原则，支持**十亿级消息存储**、**百万级并发用户**。整体设计遵循以下核心原则：

1. **数据库隔离**：每个微服务独立数据库，避免耦合
2. **分区策略**：时间序列数据采用 Range 分区，用户数据采用 Hash 分区
3. **CAS 去重**：文件系统基于内容寻址，自动去重
4. **CRDT 协同**：文档系统支持无冲突复制数据类型
5. **事件溯源**：关键业务采用事件日志，支持审计回放

---

## 🗄️ 数据库清单

| 序号 | 数据库名 | 用途 | 核心表数量 | 预估数据量 |
|------|----------|------|------------|------------|
| 1 | auth_db | 用户认证与账户 | 8 | 千万级用户 |
| 2 | im_db | 即时通讯系统 | 10 | 百亿级消息 |
| 3 | relationship_db | 好友关系 | 8 | 亿级关系 |
| 4 | file_db | 文件存储（CAS） | 8 | 千万级文件 |
| 5 | doc_db | 文档协同 | 10 | 百万级文档 |
| 6 | org_db | 组织权限 | 11 | 千万级权限 |
| 7 | event_db | 消息队列与事件 | 11 | 十亿级事件 |
| 8 | search_db | 搜索与缓存 | 12 | 亿级索引 |

---

## 🏛️ 核心架构特性

### 1️⃣ 分区表设计

#### 时间分区（Range Partition）
- **消息表** (`messages`)：按月分区，老分区使用 BRIN 索引
- **登录日志** (`login_logs`)：按月分区，自动归档
- **事件日志** (`event_logs`)：按月分区，支持审计

#### 哈希分区（Hash Partition）
- **消息收件箱** (`message_inbox`)：32个分区，均匀分布
- **用户会话** (`user_sessions`)：16个分区，提高并发

### 2️⃣ 索引策略

```sql
-- B-Tree索引：精确查询
CREATE INDEX idx_users_email ON users(email);

-- BRIN索引：时间范围查询（节省95%空间）
CREATE INDEX messages_2024_01_brin ON messages_2024_01 USING BRIN (created_at);

-- GIN索引：全文搜索
CREATE INDEX idx_message_search_tsv ON message_search_index USING GIN (content_tsv);

-- 部分索引：条件查询
CREATE INDEX idx_inbox_user_unread ON message_inbox(user_id, created_at DESC) 
    WHERE is_read = FALSE AND is_deleted = FALSE;
```

### 3️⃣ 混合消息投递模型

```
┌─────────────┬──────────────┬────────────────┐
│  场景       │  投递模式    │  存储策略      │
├─────────────┼──────────────┼────────────────┤
│ 单聊/小群   │ 写扩散       │ message_inbox  │
│ (<500人)    │ (Push)       │ 每人一份副本   │
├─────────────┼──────────────┼────────────────┤
│ 大群/频道   │ 读扩散       │ messages       │
│ (>500人)    │ (Pull)       │ 共享时间线     │
└─────────────┴──────────────┴────────────────┘
```

### 4️⃣ CAS 文件去重

```sql
-- 文件元数据表（基于SHA-256去重）
CREATE TABLE file_metadata (
    file_hash VARCHAR(64) PRIMARY KEY,  -- SHA-256作为主键
    ref_count INT DEFAULT 1,            -- 引用计数
    storage_path VARCHAR(500),          -- MinIO路径: cas/a1/b2/hash
    ...
);
```

**秒传逻辑**：
1. 客户端计算文件 SHA-256
2. 查询 `file_metadata` 表
3. 命中 → 增加引用计数 → 秒传成功
4. 未命中 → 生成预签名URL → 直传MinIO

### 5️⃣ CRDT 文档协同

```sql
-- 文档操作日志（Yjs二进制）
CREATE TABLE doc_operations (
    operation_id VARCHAR(100) UNIQUE,
    yjs_update BYTEA NOT NULL,       -- Yjs Update
    vector_clock JSONB,              -- 向量时钟
    lamport_timestamp BIGINT,        -- Lamport时间戳
    ...
);
```

### 6️⃣ 事务消息保障

```sql
-- 本地消息表（事务消息）
CREATE TABLE local_messages (
    message_id VARCHAR(100) UNIQUE,
    status SMALLINT DEFAULT 0,  -- 0:待发送, 1:已发送, 2:已确认
    retry_count INT DEFAULT 0,
    ...
);
```

**事务流程**：
1. 业务操作 + 插入本地消息表（同一事务）
2. 事务提交后发送MQ
3. 失败重试 + 幂等消费

---

## 🔗 跨库关联设计

虽然采用微服务数据库隔离，但通过以下机制实现数据关联：

### 1. 用户ID全局唯一
- 所有库的 `user_id` 字段统一使用 `auth_db.users.id`
- 采用雪花算法生成，保证全局唯一性

### 2. 冗余关键信息
```sql
-- IM库冗余用户昵称（避免跨库JOIN）
CREATE TABLE channel_members (
    user_id BIGINT,
    nickname VARCHAR(50),  -- 冗余自user_profiles
    ...
);
```

### 3. 事件驱动同步
- CDC监听 `auth_db` 变更
- 通过 RocketMQ 同步到其他库
- 最终一致性保证

---

## 🚀 部署指南

### 1. 环境要求

- PostgreSQL 14+
- 至少 32GB 内存
- SSD 存储（IOPS > 10000）
- 启用 pg_partman 扩展（自动分区管理）

### 2. 初始化顺序

```bash
# 1. 创建数据库
psql -U postgres -f 1-用户认证数据库.sql
psql -U postgres -f 2-即时通讯数据库.sql
psql -U postgres -f 3-好友关系数据库.sql
psql -U postgres -f 4-文件存储数据库.sql
psql -U postgres -f 5-文档协同数据库.sql
psql -U postgres -f 6-组织权限数据库.sql
psql -U postgres -f 7-消息队列事件数据库.sql
psql -U postgres -f 8-搜索缓存数据库.sql

# 2. 创建分区（示例）
./scripts/create_partitions.sh

# 3. 初始化权限数据
psql -U postgres -d org_db -f init_permissions.sql
```

### 3. 连接池配置

```yaml
# HikariCP 推荐配置
hikari:
  maximum-pool-size: 20      # IM服务可设置为50
  minimum-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

### 4. 定期维护

```bash
# 每日凌晨执行
0 2 * * * psql -c "VACUUM ANALYZE messages;"
0 3 * * * psql -c "REINDEX INDEX CONCURRENTLY idx_messages_channel_seq;"

# 每月归档
0 0 1 * * ./archive_old_partitions.sh
```

---

## 📊 性能优化建议

### 1. 查询优化

```sql
-- ❌ 错误：全表扫描
SELECT * FROM messages WHERE content LIKE '%keyword%';

-- ✅ 正确：使用全文搜索
SELECT * FROM messages WHERE content_tsv @@ plainto_tsquery('keyword');
```

### 2. 批量操作

```sql
-- 使用COPY而非INSERT
COPY messages FROM '/tmp/messages.csv' WITH (FORMAT csv);

-- 批量更新使用CTE
WITH updated AS (
    SELECT id FROM users WHERE status = 0 LIMIT 1000
)
UPDATE users SET status = 1 WHERE id IN (SELECT id FROM updated);
```

### 3. 连接优化

```sql
-- 使用连接池
-- 禁用自动提交
-- 使用预编译语句
-- 合理设置 work_mem
SET work_mem = '256MB';
```

---

## 🔒 安全建议

### 1. 权限隔离

```sql
-- 为每个微服务创建独立用户
CREATE USER im_service WITH PASSWORD 'xxx';
GRANT CONNECT ON DATABASE im_db TO im_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO im_service;
```

### 2. 数据加密

```sql
-- 敏感字段加密存储
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO users (password_hash) VALUES (crypt('password', gen_salt('bf')));
```

### 3. 审计日志

```sql
-- 启用审计扩展
CREATE EXTENSION IF NOT EXISTS pgaudit;
ALTER SYSTEM SET pgaudit.log = 'all';
```

---

## 📈 扩展方案

### 1. 水平扩展

- **读写分离**：主库写，从库读
- **分片策略**：按 tenant_id 或 user_id 分片
- **联邦查询**：使用 postgres_fdw 跨库查询

### 2. 垂直扩展

- **表空间分离**：热数据 SSD，冷数据 HDD
- **列式存储**：历史数据迁移到 TimescaleDB
- **内存表**：热点数据使用 pg_shared_memory

---

## 🛠️ 运维工具

### 监控查询

```sql
-- 查看表大小
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables 
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 查看慢查询
SELECT 
    query,
    calls,
    mean_exec_time,
    total_exec_time
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC 
LIMIT 10;

-- 查看锁等待
SELECT 
    blocked_locks.pid AS blocked_pid,
    blocking_locks.pid AS blocking_pid,
    blocked_activity.query AS blocked_query,
    blocking_activity.query AS blocking_query
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_locks blocking_locks 
    ON blocking_locks.locktype = blocked_locks.locktype
WHERE NOT blocked_locks.granted;
```

---

## 📝 注意事项

1. **分区表主键**：必须包含分区键
2. **JSONB vs JSON**：优先使用 JSONB（支持索引）
3. **UUID vs BIGSERIAL**：消息ID使用雪花算法，用户ID使用 BIGSERIAL
4. **软删除**：使用 deleted_at 字段而非物理删除
5. **时区处理**：统一使用 TIMESTAMPTZ

---

## 🔄 版本管理

使用 Flyway 或 Liquibase 管理数据库版本：

```sql
-- V1__Initial_schema.sql
-- V2__Add_message_reactions.sql
-- V3__Create_partitions_202412.sql
```

---

## 📞 技术支持

- **性能问题**：检查执行计划 (`EXPLAIN ANALYZE`)
- **死锁问题**：调整事务隔离级别
- **存储问题**：启用压缩 (`ALTER TABLE SET (compression = pglz)`)

---

## 作者

System Architect - 2024.11

基于架构蓝图设计，融合了 Discord、Slack、飞书等顶级产品的数据库设计精髓。
