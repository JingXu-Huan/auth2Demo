# 🚀 企业级协作平台 - 技术架构设计

> **对标飞书/Lark的新一代企业协作平台** - 集即时通讯、文档协同、智能搜索、AI助手于一体

---

## 📖 项目概述

这是一个基于**多语言微服务架构**的企业级协作平台完整技术方案，采用**Java Spring Boot + Node.js + PostgreSQL + RocketMQ + Redis + Neo4j + Elasticsearch + MinIO**技术栈，实现了：

- ✅ **亿级消息存储** - 基于PostgreSQL分区表 + 读写扩散混合架构
- ✅ **毫秒级实时协同** - 基于Yjs CRDT算法的Node.js Sidecar服务
- ✅ **金融级消息可靠性** - RocketMQ事务消息 + 严格顺序保障
- ✅ **图谱化权限控制** - Neo4j实现O(1)复杂度的RBAC/ReBAC
- ✅ **智能RAG搜索** - Elasticsearch混合检索 + 向量语义搜索
- ✅ **秒传与去重** - CAS内容寻址存储 + MinIO对象存储

---

## 📂 文档导航

### 🏗️ 核心设计文档
| 文档 | 说明 | 阅读优先级 |
|------|------|:------:|
| [架构蓝图.md](架构蓝图.md) | 核心架构哲学、技术选型深度剖析 | ⭐⭐⭐⭐⭐ |
| [模块设计.md](模块设计.md) | 12个微服务的职责边界与交互流程 | ⭐⭐⭐⭐⭐ |
| [数据库设计.md](数据库设计.md) | 完整的PostgreSQL表结构与分区策略 | ⭐⭐⭐⭐ |
| [api接口.md](api接口.md) | RESTful API与WebSocket协议定义 | ⭐⭐⭐⭐ |

### 🔧 微服务设计文档
| 服务模块 | 核心职责 | 技术栈 |
|----------|----------|--------|
| [gateway-service.md](gateway-service.md) | API网关、统一鉴权、流量控制 | Spring Cloud Gateway + Sentinel |
| [auth-service.md](auth-service.md) | 身份认证、多端登录、风控封禁 | Spring Security + Redis + JustAuth |
| [user-service.md](user-service.md) | 组织架构、物化路径、通讯录 | MyBatis Plus + 事件驱动 |
| [im-gateway.md](im-gateway.md) | 长连接管理、C10K、消息下发 | Netty + WebSocket + Protobuf |
| [im-service.md](im-service.md) | 消息发送、SeqID生成、会话管理 | RocketMQ事务消息 + Redis |
| [collab-service.md](collab-service.md) | 文档协同、CRDT算法、实时广播 | **Node.js + Yjs + Redis Stream** |
| [file-service.md](file-service.md) | 文件上传、CAS去重、预签名URL | MinIO + SHA256 |
| [search-service.md](search-service.md) | 全文检索、RAG、MCP协议 | Elasticsearch + LangChain4j |
| [sync-worker.md](sync-worker.md) | 异步数据同步、Neo4j/ES索引 | RocketMQ Consumer + Redis Stream |
| [admin-service.md](admin-service.md) | 管理后台、审计日志、敏感词 | Spring Boot + Feign |
| [job-service.md](job-service.md) | 定时任务、分区维护、数据清理 | XXL-JOB |

### 🩹 补充设计
- [补丁.md](补丁.md) - IM话题(Thread)与文档评论的数据库设计

---

## 🏛️ 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                       客户端层 (Client Layer)                     │
│   Web (React) │ Desktop (Electron) │ Mobile (Flutter/Native)    │
└────────────────────┬────────────────────────────────────────────┘
                     │
         ┌───────────▼──────────────┐
         │  Nginx (L4/L7 LB)        │
         └───────────┬──────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
┌───▼────┐      ┌───▼─────┐     ┌───▼────────┐
│Gateway │      │IM Gateway│    │Collab (WS) │
│Service │      │  (Netty) │    │  (Node.js) │
└───┬────┘      └────┬─────┘     └─────┬──────┘
    │                │                  │
    └────────┬───────┴──────────────────┘
             │
    ┌────────▼─────────────────────────────────┐
    │       微服务层 (Business Services)        │
    │  Auth │ User │ IM │ File │ Search │...   │
    └────────┬─────────────────────────────────┘
             │
    ┌────────▼─────────────────────────────────┐
    │      数据与消息层 (Data & MQ Layer)       │
    ├──────────────────────────────────────────┤
    │ PostgreSQL(分区) │ Redis(缓存/SeqID)     │
    │ RocketMQ(可靠消息)│ Neo4j(权限图谱)      │
    │ Elasticsearch(搜索)│ MinIO(对象存储)     │
    └──────────────────────────────────────────┘
```

---

## 🎯 核心技术亮点

### 1. 混合架构 - 读写扩散智能切换
- **小群(<500人)**: 写扩散(Inbox) → 用户拉取O(1)
- **大群(>500人)**: 读扩散(Timeline) → 避免写放大
- **在线推送**: WebSocket实时下发，离线拉取

### 2. 海量数据存储 - PostgreSQL分区表
```sql
-- 按月自动分区
CREATE TABLE chat_messages_2025_11 PARTITION OF chat_messages
FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

-- 热数据B-Tree，冷数据BRIN索引
CREATE INDEX ON chat_messages_2025_11 (channel_id, seq_id DESC);
CREATE INDEX ON chat_messages_2024_01 USING BRIN (created_at); -- 冷分区
```

### 3. 实时协同 - Yjs + Node.js Sidecar
```javascript
// Node.js处理毫秒级CRDT合并
doc.on('update', (update) => {
  // 1. 本地广播
  broadcastToClients(update);
  // 2. 跨节点广播 (Redis Pub/Sub)
  redis.publish(`yjs:${docId}`, update);
  // 3. 异步持久化 (Redis Stream -> Java Worker)
  redis.xadd('yjs:stream', '*', 'docId', docId, 'update', update);
});
```

### 4. 事务消息 - 数据库与MQ的原子性
```java
// RocketMQ 2PC保障一致性
@Transactional
public void sendMessage(Msg msg) {
    // 1. Half Message -> Broker
    rocketMQ.sendMessageInTransaction("IM_TOPIC", msg);
    // 2. Local DB Transaction
    msgMapper.insert(msg);
    // 3. Commit -> 消息可见
}
```

### 5. 图谱权限 - Neo4j O(1)查询
```cypher
-- 判断用户是否有权限访问文档
MATCH (u:User {id: $uid})-[:BELONGS_TO*0..10]->(dept:Dept)
MATCH (dept)-[:HAS_ROLE]->(role:Role)-[:CAN_EDIT]->(doc:Doc {id: $docId})
RETURN count(*) > 0
```

---

## 🛠️ 技术栈汇总

### 后端技术栈
- **Java 21** - Virtual Threads, Spring Boot 3.x
- **Node.js 18+** - Yjs协同引擎
- **数据库**: PostgreSQL 15 (分区表), Neo4j 5.x (图谱)
- **缓存**: Redis Cluster 7.x (AOF持久化)
- **消息队列**: Apache RocketMQ 5.x (事务消息)
- **搜索引擎**: Elasticsearch 8.x (向量检索)
- **对象存储**: MinIO (S3兼容)
- **调度**: XXL-JOB (分布式定时任务)

### 基础设施
- **容器**: Docker + Kubernetes
- **网关**: Nginx (L4/L7负载均衡)
- **监控**: Prometheus + Grafana + SkyWalking
- **日志**: ELK Stack (Elasticsearch + Logstash + Kibana)

---

## 📊 性能指标设计目标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **消息发送延迟** | < 100ms (P99) | 从发送到存库+投递MQ |
| **实时协同延迟** | < 50ms (P99) | 用户输入到其他人看到 |
| **历史消息查询** | < 200ms | 利用分区剪枝 + BRIN索引 |
| **并发连接数** | 单节点10万+ | Netty + Epoll优化 |
| **QPS** | 网关50K+, IM-Service 20K+ | 异步处理 + MQ削峰 |
| **存储去重率** | > 60% | CAS内容寻址 |

---

## 🚦 实施路线图

### 阶段一：核心IM系统 (MVP)
- ✅ 用户认证与组织架构
- ✅ P2P/群组聊天
- ✅ PostgreSQL分区表设计
- ✅ RocketMQ事务消息

### 阶段二：协同与文件
- ⏳ Node.js协同文档服务
- ⏳ MinIO对象存储与CAS去重
- ⏳ 文件预签名上传/下载

### 阶段三：智能搜索与权限
- ⏳ Elasticsearch全文检索
- ⏳ Neo4j权限图谱
- ⏳ RAG智能问答

### 阶段四：开放平台
- 📋 Webhook机器人
- 📋 MCP协议支持
- 📋 第三方应用市场

---

## 📜 开发规范

### Git提交规范
```
feat: 新增文档协同功能
fix: 修复消息乱序问题
perf: 优化PG分区查询性能
docs: 更新API文档
```

### 代码规范
- **Java**: 遵循阿里巴巴Java开发规范
- **Node.js**: ESLint + Prettier
- **SQL**: 所有DDL必须包含注释

---

## 🤝 贡献指南

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add amazing feature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交Pull Request

---

## 📄 许可证

本项目仅供学习研究使用，请勿用于商业用途。

---

## 🙏 致谢

本架构设计参考了以下优秀开源项目与技术文章：
- **Yjs** - CRDT协同编辑算法
- **Discord** - 亿级消息存储实践
- **飞书/Lark** - 企业协作产品设计
- **Spring Cloud Alibaba** - 微服务生态

---

## 📬 联系方式

如有疑问或建议，欢迎通过Issue讨论。

**项目状态**: 🔨 架构设计完成，开发进行中...

---

<div align="center">
  <strong>⚡ 用技术连接团队，让协作更高效 ⚡</strong>
</div>
