# IM File Server - 企业级文件中台

基于 **CAS (Content Addressable Storage)** + **MinIO** 的高效、低成本文件服务。

---

## 📋 核心特性

### 1. 🎯 CAS 内容寻址存储
- **SHA-256 去重**：相同文件只存储一份
- **秒传功能**：文件已存在时瞬间完成上传
- **节省存储**：群聊场景下，多人转发同一文件只占用一份空间

### 2. 🚀 流量旁路设计
- **预签名 URL**：客户端直接与 MinIO 交互
- **零带宽消耗**：文件流不经过 Java 服务
- **高并发支持**：应用服务器只处理元数据

### 3. ♻️ 引用计数 GC
- **自动回收**：引用计数为 0 时标记待删除
- **延迟删除**：24小时冷却期，防止误删
- **定时清理**：每天凌晨 2 点自动执行

### 4. 🖼️ 多媒体处理
- **缩略图生成**：图片自动生成 200x200 缩略图
- **格式转换**：统一输出 JPEG 格式
- **延迟加载**：按需生成，减少存储压力

---

## 🏗️ 架构设计

### 数据流图

```
┌─────────┐
│ 客户端  │
└────┬────┘
     │ 1. Pre-upload (SHA256)
     ↓
┌─────────────────┐
│ File Service    │ ←─→ PostgreSQL (元数据)
└────┬──────┬─────┘
     │      │ 2. Gen Presigned URL
     │      ↓
     │  ┌──────────┐
     │  │  MinIO   │
     │  └────┬─────┘
     │       │ 3. PUT Binary
     └───────┼──────→ 客户端直传
             │
             ↓
         物理存储
```

### 核心流程

#### 上传流程
1. **前端计算 SHA-256**
2. **调用预上传接口**
   - 命中：返回 `EXIST`（秒传）
   - 未命中：返回预签名 PUT URL
3. **客户端直传 MinIO**
4. **调用确认接口**
5. **后端保存元数据**

#### 下载流程
1. **请求下载 URL**
2. **查询 Redis 缓存**
3. **生成预签名 GET URL**
4. **客户端直接从 MinIO 下载**

---

## 📦 数据库设计

### file_metadata（物理文件表）
| 字段 | 类型 | 说明 |
|------|------|------|
| file_hash | VARCHAR(64) | SHA-256（主键） |
| file_size | BIGINT | 文件大小 |
| storage_path | VARCHAR(255) | MinIO 对象键 |
| ref_count | INT | 引用计数 |
| status | INT | 1=正常, 2=待GC |
| extra | JSONB | 扩展信息（缩略图路径等） |

### user_files（用户逻辑表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| user_id | BIGINT | 上传者 |
| file_hash | VARCHAR(64) | 关联物理文件 |
| file_name | VARCHAR(255) | 原始文件名 |
| source_type | INT | 来源：1=IM, 2=文档 |
| deleted_at | TIMESTAMPTZ | 软删除时间 |

---

## 🔌 API 接口

### 1. 预上传检查
```http
POST /api/file/pre-upload
Content-Type: application/json
X-User-Id: 123

{
  "hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "size": 1024000,
  "fileName": "photo.jpg",
  "ext": "jpg"
}
```

**响应（秒传）**：
```json
{
  "code": 200,
  "data": {
    "status": "EXIST",
    "fileHash": "e3b0c44...",
    "fileId": 456
  }
}
```

**响应（需上传）**：
```json
{
  "code": 200,
  "data": {
    "status": "MISS",
    "uploadUrl": "http://minio:9000/im-files/cas/e3/b0/...?X-Amz-Signature=...",
    "objectKey": "cas/e3/b0/e3b0c44...jpg",
    "fileHash": "e3b0c44..."
  }
}
```

### 2. 确认上传
```http
POST /api/file/confirm
Content-Type: application/json
X-User-Id: 123

{
  "hash": "e3b0c44...",
  "objectKey": "cas/e3/b0/e3b0c44...jpg",
  "fileName": "photo.jpg",
  "size": 1024000,
  "ext": "jpg"
}
```

### 3. 获取下载 URL
```http
GET /api/file/url/{fileHash}
X-User-Id: 123
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "url": "http://minio:9000/im-files/cas/e3/b0/...?X-Amz-Signature=..."
  }
}
```

### 4. 获取预览 URL
```http
GET /api/file/preview/{fileHash}
X-User-Id: 123
```

---

## ⚙️ 配置说明

### application.yml

```yaml
# MinIO 配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: im-files

# 数据库配置
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/im_file
    username: postgres
    password: your_password

# Redis 配置
  redis:
    host: localhost
    port: 6379
```

### MinIO 初始化

```bash
# 创建存储桶
mc mb minio/im-files

# 设置为私有
mc policy set none minio/im-files
```

---

## 🚀 快速启动

### 1. 启动依赖服务

```bash
# PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:14

# Redis
docker run -d --name redis -p 6379:6379 redis:7

# MinIO
docker run -d --name minio \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

### 2. 初始化数据库

```bash
psql -U postgres -f db/init.sql
```

### 3. 启动服务

```bash
mvn spring-boot:run
```

---

## 📊 性能优化

### 1. 缓存策略
- **下载 URL**：Redis 缓存 50 分钟
- **预览 URL**：Redis 缓存 50 分钟
- **元数据**：MyBatis 二级缓存

### 2. 并发控制
- **数据库连接池**：HikariCP（最大 10 连接）
- **Redis 连接池**：Lettuce（最大 8 连接）

### 3. 大文件处理
- **分片上传**：>100MB 使用 Multipart Upload
- **断点续传**：支持 Range 请求

---

## 🔒 安全设计

### 1. 权限控制
- **私有存储桶**：所有文件必须通过签名访问
- **临时凭证**：预签名 URL 有效期 10-60 分钟
- **用户隔离**：通过 user_files 表关联

### 2. 防滥用
- **文件大小限制**：单文件最大 500MB（可配置）
- **上传频率限制**：每用户每分钟最多 10 次
- **存储配额**：每用户最大 10GB

---

## 🛠️ 运维工具

### 查看存储统计

```sql
-- 总文件数
SELECT COUNT(*) FROM file_metadata WHERE status = 1;

-- 总存储大小
SELECT pg_size_pretty(SUM(file_size)::BIGINT) FROM file_metadata WHERE status = 1;

-- 引用计数分布
SELECT ref_count, COUNT(*) 
FROM file_metadata 
GROUP BY ref_count 
ORDER BY ref_count;
```

### 手动触发 GC

```bash
curl -X POST http://localhost:8004/api/admin/gc
```

---

## 📝 TODO

- [ ] 支持视频转码
- [ ] 支持文档预览（PDF/Word）
- [ ] CDN 集成
- [ ] 分片上传前端 SDK
- [ ] 存储配额管理
- [ ] 审计日志

---

## 📄 License

MIT License
