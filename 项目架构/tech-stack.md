# 🛠️ 技术栈全景图

本文档详细列举了企业级协作平台所使用的所有技术组件、版本选型依据及替代方案。

---

## 📊 技术架构总览

```
┌─────────────────────────────────────────────────────────┐
│                      前端技术栈                          │
│  React 18 + TypeScript + TailwindCSS + Zustand        │
│  Prosemirror (富文本) + Yjs (CRDT)                     │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    网关与接入层                          │
│  Nginx (L4/L7) + Spring Cloud Gateway + Sentinel       │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    微服务应用层                          │
│  Spring Boot 3.x (Java) + Node.js 18 (协同)            │
│  Netty (长连接) + Fastify (Node框架)                    │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                  数据与消息中间件                        │
│  PostgreSQL + Redis + RocketMQ + Elasticsearch         │
│  Neo4j + MinIO                                          │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    基础设施层                            │
│  Docker + Kubernetes + Prometheus + ELK                │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 核心技术栈详解

### 1️⃣ 后端开发框架

#### Spring Boot 3.x
**版本**: 3.2.0+  
**用途**: 业务微服务主框架  
**核心依赖**:
```xml
<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- WebFlux (Gateway使用) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Actuator (监控) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

**选型理由**:
- ✅ 成熟的企业级生态
- ✅ 原生支持分布式追踪 (Micrometer)
- ✅ Virtual Threads (Java 21) 大幅提升IO性能
- ✅ AOT编译支持,启动速度提升

**替代方案**: Quarkus, Micronaut (更轻量,但生态不如Spring成熟)

---

#### Node.js 18 LTS
**用途**: 实时协同引擎 (collab-service)  
**核心库**:
```json
{
  "dependencies": {
    "fastify": "^4.25.0",
    "ws": "^8.16.0",
    "yjs": "^13.6.0",
    "y-protocols": "^1.0.6",
    "ioredis": "^5.3.0",
    "jsonwebtoken": "^9.0.2"
  }
}
```

**选型理由**:
- ✅ Yjs原生JavaScript实现,性能最优
- ✅ 非阻塞IO天然适合高并发WebSocket
- ✅ V8引擎对CRDT数据结构优化极佳

**替代方案**: Deno (更现代,但Yjs生态不完善)

---

### 2️⃣ 数据存储

#### PostgreSQL 15
**用途**: 核心业务数据库  
**关键特性**:
- 🔹 **分区表 (Partitioning)**: 按月自动分区消息表
- 🔹 **BRIN索引**: 冷数据低成本索引
- 🔹 **JSONB**: 存储灵活的扩展字段
- 🔹 **WAL流复制**: 主从高可用

**配置要点**:
```sql
-- 开启分区剪枝
SET enable_partition_pruning = on;

-- 大表统计信息
ALTER TABLE chat_messages SET (autovacuum_analyze_threshold = 100000);

-- BRIN索引示例
CREATE INDEX ON chat_messages_2023_01 USING BRIN (created_at);
```

**容量规划**:
- 1亿条消息 ≈ 50GB (含索引)
- 分区表每月一个分区
- 冷数据迁移到廉价存储

**替代方案**: 
- TiDB (分布式,但运维复杂度高)
- Cassandra (极致扩展性,但失去事务一致性)

---

#### Redis Cluster 7.x
**用途**: 缓存 + SeqID生成 + 会话管理  
**集群模式**: 3主3从  
**关键数据结构**:
- **String**: 用户Token, 封禁标记
- **Hash**: 未读数统计 `unread:{uid}`
- **Stream**: Yjs持久化缓冲
- **Pub/Sub**: 跨节点消息广播

**性能指标**:
- QPS: 单节点10万+
- 延迟: P99 < 1ms
- 持久化: AOF + RDB混合模式

**替代方案**: Dragonfly (兼容Redis协议,性能更高)

---

#### Neo4j 5.x
**用途**: 组织架构权限图谱  
**数据模型**:
```cypher
// 节点
(:User {id, name})
(:Department {id, name, path})
(:Role {id, name})
(:Resource {id, type})

// 关系
(:User)-[:BELONGS_TO]->(:Department)
(:Department)-[:SUB_DEPT_OF]->(:Department)
(:Role)-[:CAN_READ/CAN_EDIT]->(:Resource)
```

**查询性能**:
- 权限检查: < 10ms (3跳路径查询)
- 复杂组织关系: O(1) vs SQL的O(N)

**替代方案**: JanusGraph (分布式图数据库,但学习曲线陡)

---

#### Elasticsearch 8.x
**用途**: 全文检索 + 向量搜索 + 审计日志  
**核心功能**:
- **IK分词器**: 中文智能分词
- **kNN向量检索**: 语义搜索
- **聚合查询**: 报表统计

**索引策略**:
```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "30s",
    "analysis": {
      "analyzer": {
        "ik_max_word": { "type": "ik_max_word" }
      }
    }
  }
}
```

**替代方案**: Meilisearch (轻量级,但功能不如ES强大)

---

#### MinIO
**用途**: 对象存储 (文件/图片/视频)  
**部署模式**: 分布式纠删码 (4节点)  
**关键特性**:
- S3兼容API
- CAS内容寻址去重
- Presigned URL直传

**成本优势**:
- 相比阿里云OSS节省70%成本
- 10TB数据月成本 < ¥500

**替代方案**: 
- Ceph (功能更强,但运维成本高)
- 公有云OSS (简单但费用高)

---

### 3️⃣ 消息队列

#### Apache RocketMQ 5.x
**用途**: 异步解耦 + 事务消息 + 顺序消息  
**核心Topic**:
| Topic | 用途 | 消费模式 |
|-------|------|----------|
| `IM_MSG_SEND` | IM消息投递 | 广播 (Broadcasting) |
| `USER_EVENT` | 用户变更 | 集群 (Clustering) |
| `ORG_EVENT` | 组织架构变更 | 有序 (Orderly) |
| `DOC_UPDATE` | 文档更新 | 集群 |

**事务消息流程**:
```
1. Half Message (半消息)
2. Local DB Transaction (本地事务)
3. Commit/Rollback (提交/回滚)
4. Transaction Check (事务回查)
```

**性能指标**:
- 吞吐量: 单机10万TPS
- 延迟: P99 < 100ms
- 可靠性: 99.99%

**替代方案**: 
- Kafka (吞吐量更高,但无事务消息)
- Pulsar (云原生,但社区不如RocketMQ成熟)

---

### 4️⃣ 网关与负载均衡

#### Nginx
**用途**: L4/L7负载均衡 + SSL卸载  
**配置示例**:
```nginx
upstream gateway_backend {
    ip_hash;  # 会话保持
    server 10.0.2.10:8080 max_fails=3 fail_timeout=30s;
    server 10.0.2.11:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;  # 长连接池
}

server {
    listen 443 ssl http2;
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    
    location / {
        proxy_pass http://gateway_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }
}
```

**替代方案**: HAProxy, Traefik

---

#### Spring Cloud Gateway
**用途**: 微服务网关 (鉴权/限流/路由)  
**核心过滤器**:
- `AuthGlobalFilter`: JWT验签 + Redis风控
- `SentinelGatewayFilter`: 流量控制
- `GrayReleaseFilter`: 灰度发布

**性能指标**:
- QPS: 5万+ (4核8G)
- P99延迟: < 50ms

**替代方案**: Kong, APISIX (功能更强,但学习成本高)

---

### 5️⃣ 长连接网关

#### Netty 4.1
**用途**: IM长连接网关 (WebSocket/TCP)  
**核心Handler链**:
```java
pipeline.addLast(new IdleStateHandler(180, 0, 0))
        .addLast(new HttpServerCodec())
        .addLast(new WebSocketServerProtocolHandler("/ws"))
        .addLast(new ProtobufDecoder(IMPacket.getDefaultInstance()))
        .addLast(new AuthHandler())
        .addLast(new BusinessHandler());
```

**性能调优**:
- 单节点支持10万+ TCP连接
- 使用Epoll (Linux原生IO)
- PooledByteBufAllocator (池化内存)

**替代方案**: uWebSockets (C++实现,性能极致但生态不如Netty)

---

### 6️⃣ 协同编辑

#### Yjs
**用途**: CRDT算法库  
**核心概念**:
- **Y.Doc**: 文档状态容器
- **Update**: 增量变更二进制流
- **Awareness**: 光标与在线状态

**数据流**:
```
Client A Edit -> Update -> WebSocket -> Node.js Server
                                       ├─> Broadcast to Client B
                                       ├─> Redis Pub/Sub (跨节点)
                                       └─> Redis Stream (持久化)
```

**替代方案**: 
- Automerge (Rust实现,性能更好但生态不如Yjs)
- OT算法 (Operational Transformation,实现复杂度高)

---

### 7️⃣ 任务调度

#### XXL-JOB
**用途**: 分布式定时任务调度  
**核心任务**:
- PG分区自动创建
- MinIO文件GC
- 日活统计报表
- 数据一致性对账

**调度模式**:
- CRON: 固定时间执行
- 固定间隔: 每N秒执行
- 分片广播: 大数据量并行处理

**替代方案**: Quartz (单机), Elastic-Job (分片支持)

---

### 8️⃣ 监控与日志

#### Prometheus + Grafana
**指标采集**:
- JVM: 堆内存、GC、线程
- 业务: QPS、延迟、错误率
- 中间件: Redis连接数、PG TPS

**告警规则**:
```yaml
- alert: HighAPILatency
  expr: histogram_quantile(0.99, http_request_duration_seconds_bucket) > 1
  for: 5m
  annotations:
    summary: "API P99延迟超过1秒"
```

---

#### ELK Stack
**日志采集**:
```
Application (Log4j2/Logback)
  -> Filebeat (采集)
  -> Logstash (解析)
  -> Elasticsearch (存储)
  -> Kibana (可视化)
```

**日志格式** (JSON):
```json
{
  "timestamp": "2025-11-24T10:00:00Z",
  "level": "ERROR",
  "service": "im-service",
  "traceId": "abc123",
  "message": "发送消息失败",
  "exception": "..."
}
```

---

#### SkyWalking
**用途**: 分布式链路追踪  
**追踪链路示例**:
```
Gateway -> Auth-Service -> User-Service -> PostgreSQL
        -> IM-Service -> RocketMQ -> IM-Gateway
```

**关键指标**:
- 调用拓扑图
- 端到端延迟分解
- 慢查询定位

---

### 9️⃣ AI与搜索

#### LangChain4j
**用途**: Java版LLM应用框架  
**核心功能**:
- Embedding模型集成 (bge-m3, OpenAI)
- RAG检索增强生成
- MCP协议服务端

**代码示例**:
```java
EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();
Response<Embedding> response = model.embed("用户消息");
List<Float> vector = response.content().vectorAsList();
```

---

#### Apache Tika
**用途**: 文档内容提取  
**支持格式**:
- Office: docx, xlsx, pptx
- PDF, TXT, Markdown
- 图片OCR (Tesseract)

---

### 🔟 容器编排

#### Kubernetes 1.28+
**资源对象**:
- Deployment: 无状态服务
- StatefulSet: IM Gateway (需要固定IP)
- ConfigMap/Secret: 配置管理
- HPA: 自动伸缩

**存储方案**:
- PV/PVC: PostgreSQL数据持久化
- StorageClass: 动态卷分配

---

## 📦 开发工具链

### 版本控制
- **Git** + **GitLab/GitHub**
- 分支策略: GitFlow (main, develop, feature/*)

### CI/CD
- **Jenkins** / **GitLab CI**
- 流程: 代码提交 -> 单元测试 -> 构建镜像 -> 推送仓库 -> K8s部署

### 代码质量
- **SonarQube**: 静态代码扫描
- **JaCoCo**: 覆盖率检测
- **ESLint/Prettier**: 前端代码规范

### API文档
- **Swagger/OpenAPI 3.0**
- **Postman**: 接口测试

---

## 🎨 前端技术栈

### 核心框架
- **React 18** + **TypeScript**
- **Vite**: 构建工具
- **TailwindCSS**: 原子化CSS
- **shadcn/ui**: 组件库

### 状态管理
- **Zustand**: 轻量级状态管理
- **React Query**: 服务端状态管理

### 富文本编辑
- **Prosemirror**: 富文本引擎
- **y-prosemirror**: Yjs绑定
- **Tiptap**: Prosemirror封装层

### WebSocket
- **Socket.io-client** (备选)
- **原生WebSocket** (主用)

---

## 📊 技术选型对比表

| 维度 | 方案A | 方案B | 最终选择 | 理由 |
|------|-------|-------|----------|------|
| **数据库** | PostgreSQL | MySQL | PostgreSQL | 分区表支持更好,JSONB性能优秀 |
| **消息队列** | RocketMQ | Kafka | RocketMQ | 事务消息,顺序消息原生支持 |
| **缓存** | Redis | Memcached | Redis | 数据结构丰富,Stream支持 |
| **搜索** | Elasticsearch | Solr | Elasticsearch | 向量检索,社区活跃 |
| **对象存储** | MinIO | Ceph | MinIO | S3兼容,运维简单 |
| **协同引擎** | Yjs | OT算法 | Yjs | 算法简单,性能优秀 |
| **长连接** | Netty | Tomcat NIO | Netty | C10K支持,性能极致 |

---

## 🔄 技术演进路线

### 短期 (0-6个月)
- ✅ 完成核心IM功能
- ✅ PostgreSQL分区表上线
- ✅ RocketMQ事务消息

### 中期 (6-12个月)
- 🔄 Node.js协同服务
- 🔄 Neo4j权限图谱
- 🔄 Elasticsearch全文检索

### 长期 (12个月+)
- 📋 AI RAG问答
- 📋 MCP协议支持
- 📋 WASM插件系统

---

## 🎓 学习资源

### 官方文档
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Yjs Docs](https://docs.yjs.dev/)
- [RocketMQ Docs](https://rocketmq.apache.org/docs/)

### 推荐书籍
- 《深入理解Kafka》
- 《高性能MySQL》
- 《PostgreSQL修炼之道》
- 《Netty权威指南》

### 开源项目参考
- **Rocket.Chat** - 开源IM
- **OnlyOffice** - 协同文档
- **Zulip** - 企业级聊天

---

## 📝 版本信息

| 组件 | 生产版本 | 更新日期 |
|------|----------|----------|
| Spring Boot | 3.2.0 | 2024-11 |
| Node.js | 18.19.0 LTS | 2024-10 |
| PostgreSQL | 15.5 | 2024-11 |
| Redis | 7.2.3 | 2024-09 |
| RocketMQ | 5.1.4 | 2024-08 |
| Elasticsearch | 8.11.1 | 2024-10 |
| Neo4j | 5.14.0 | 2024-11 |

---

<div align="center">
  <strong>🚀 技术栈持续演进，以适应业务发展 🚀</strong>
</div>
