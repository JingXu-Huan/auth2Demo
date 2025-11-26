# Collab-Service

基于 **Node.js + Yjs + Redis** 的实时协同编辑 Sidecar 服务，用于支撑类似 Feishu Docs / Google Docs 的毫秒级协同体验。

## 1. 定位与职责

- **纯计算、软状态（Soft-State）Sidecar**
  - 不直接连接 PostgreSQL，不处理计费、目录树等复杂业务逻辑。
  - 只负责：
    - WebSocket 长连接维护
    - CRDT（Yjs）数据合并与冲突解决
    - 跨节点实时广播
    - 将增量更新写入 Redis Stream，交给 Java Sync-Worker 落库

- 与 Spring Boot 主系统配合方式：
  - Spring Boot：管文档（权限、元数据、快照持久化）。
  - Node collab-service：写文档（内容协同、冲突解决）。

## 2. 技术栈

- Runtime: **Node.js >= 18**
- HTTP 框架: **Fastify**
- WebSocket: **ws**（原生 WebSocket 库）
- CRDT 核心: **Yjs**
- Redis 客户端: **ioredis**（Pub/Sub + Stream）
- Auth: **jsonwebtoken**（本地 JWT 验签，RS256）

## 3. 架构与数据流

```mermaid
graph TD
    Client[前端 (Prosemirror/Quill)] -- WebSocket (Yjs Update) --> LB[Nginx]
    LB -- Hash/IP --> Node1[collab-service #1]
    LB -- Hash/IP --> Node2[collab-service #2]

    subgraph Collab-Service
        WS[WS Server]
        YDocMap[Memory: Map<docId, Y.Doc>]
    end

    Node1 <--> RedisPubSub[Redis Pub/Sub\n yjs:pubsub:{docId}]
    Node2 <--> RedisPubSub

    Node1 -- XADD --> RedisStream[yjs:stream]
    Node2 -- XADD --> RedisStream

    RedisStream --> JavaWorker[Async-server DocPersistenceWorker]
    JavaWorker --> PG[(PostgreSQL)]
    JavaWorker --> MinIO[(MinIO)]
```

### 3.1 握手与鉴权

- WebSocket 地址：`ws://<host>/doc/ws/{docId}?token={JWT}`
- 鉴权流程（在 `src/server.js` 中实现）：
  1. 解析 URL，提取 `docId` 和 `token`。
  2. 使用环境变量 `JWT_PUBLIC_KEY`（与 Spring Boot 共享的 RSA 公钥）做 **RS256 JWT 验签**。
  3. 解析出 `userId`（从 `user_id`/`sub`/`uid` 中择一）。
  4. 鉴权通过后，将 WebSocket 连接绑定到对应文档 `docId` 上，交由 `setupWSConnection` 处理。

> 权限模型（只读/读写）可以根据 `decoded` Token 中的 scope/resource 再做扩展，本实现先聚焦连接级鉴权。

### 3.2 内存状态与多节点广播

- 内存结构：`Map<string, WSSharedDoc>`
  - `WSSharedDoc` 内含：
    - `Y.Doc` 实例
    - 当前节点上连接到该文档的 WebSocket 集合 `Set<WebSocket>`
- 文档生命周期：
  - 第一个用户进入：创建 `WSSharedDoc`，订阅 `yjs:pubsub:{docId}`。
  - 活跃期间：所有更新都在内存 `Y.Doc` 中累积。
  - 最后一个用户离开：延迟 `DOC_UNLOAD_DELAY_MS`（默认 30s）后，若仍无连接，则销毁 `Y.Doc` 并从 Map 中删除。
- 多节点协同：
  - 本节点收到来自前端的更新 → `Y.applyUpdate(ydoc, update)` → 广播给本节点所有连接。
  - 同时：
    - 将更新以 Base64 形式 `PUBLISH` 到 `yjs:pubsub:{docId}`，由其他 Node 实例订阅并 `applyUpdate(..., 'redis')`。
    - 将更新 `XADD` 到 Redis Stream `yjs:stream`，由 Java Worker 批处理持久化。

### 3.3 持久化路径

- collab-service **不直接写 DB**，只写 Redis Stream：
  - Stream Key：`yjs:stream`
  - 字段：`docId`、`update`(Base64)
- Java 侧的 `Async-server` 中：
  - `DocPersistenceWorker` 以 Consumer Group 方式消费 `yjs:stream`。
  - 对同一 `docId` 的更新在内存中合并后，调用后端逻辑写入 PostgreSQL/MinIO（当前为 TODO 占位）。

## 4. 启动与配置

### 4.1 安装依赖

```bash
cd collab-service
npm install
```

### 4.2 必要环境变量

- `JWT_PUBLIC_KEY`：与 Spring Boot 使用的 RS256 公钥一致（PEM 字符串）。
- `REDIS_URL`：Redis 地址，例如 `redis://127.0.0.1:6379`。

可选：

- `PORT`：服务监听端口，默认 `3001`。
- `DOC_UNLOAD_DELAY_MS`：文档无连接后延迟卸载时间，默认 `30000` 毫秒。

### 4.3 启动

开发模式（含热重启）：

```bash
npm run dev
```

生产模式：

```bash
npm start
```

## 5. 后续可扩展点

- 接入 `y-protocols` 的 `sync` 和 `awareness` 协议：
  - 兼容官方 `y-websocket` 客户端，实现 SyncStep1/2、Awareness 光标同步等。
- 从 Java 后端加载初始 Snapshot：
  - 首个连接建立时，通过 HTTP/RPC 获取二进制快照，初始化 `Y.Doc`。
- 更细粒度的权限控制：
  - 基于 JWT 中的 scope/resource 对文档设置只读/读写标记。

当前实现已满足：Node.js 侧负责 **长连接 + CRDT 合并 + 跨节点广播 + 写 Redis Stream**，Java Sync-Worker 负责 **批处理落库**，整体架构完全符合“快慢分离”的设计目标。
