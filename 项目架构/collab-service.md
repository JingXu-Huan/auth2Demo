该服务是整个平台中**技术门槛最高、对延迟最敏感**的组件。为了实现类似 Feishu Docs（飞书文档）或 Google Docs 的毫秒级协同体验，我们必须打破常规的 CRUD 模式，采用 **Node.js + CRDT (Yjs) + Redis** 的高性能组合。

---

### 1. 核心定位与设计哲学
+ **定位**：**纯计算、软状态（Soft-State）的 Sidecar 服务**。
    - 它**不直接**连接 PostgreSQL，不处理复杂的业务逻辑（如计费、目录树）。
    - 它**只负责** WebSocket 长连接维护、CRDT 数据合并、冲突解决和即时广播。
+ **为什么选 Node.js?**
    - **V8 引擎优化**：Yjs 核心库是基于 JavaScript 编写的，V8 引擎对对象的创建和内存管理进行了极致优化，处理 CRDT 的性能远超 Java。
    - **非阻塞 I/O**：WebSocket 高并发广播是 Node.js 的强项。
+ **为什么是 Sidecar?**
    - 它像一个“外挂引擎”，挂载在 Spring Boot 主系统旁。Spring Boot 负责“管文档”（权限、元数据），Node.js 负责“写文档”（内容）。

---

### 2. 技术栈清单
+ **Runtime**: Node.js v18+ (LTS)
+ **Web Framework**: Fastify (比 Express 快 5 倍，适合高吞吐)
+ **WebSocket**: `ws` (原生 WebSocket 库，性能最佳) 或 `uWebSockets.js` (C++ 绑定，追求极致)
+ **CRDT Core**: `yjs` (核心算法), `y-websocket` (协议适配)
+ **Messaging**: `ioredis` (处理 Redis Stream 和 Pub/Sub)
+ **Auth**: `jsonwebtoken` (本地 JWT 验签)

---

### 3. 架构拓扑图
```mermaid
graph TD
    Client[前端 (Prosemirror/Quill)] -- WebSocket (Update) --> LB[Nginx 负载均衡]
    LB -- Hash/IP --> Node1[Node.js 实例 1]
    LB -- Hash/IP --> Node2[Node.js 实例 2]

    subgraph Node.js Internal
        WS[WS Server]
        YDocMap[Memory: Map<DocID, Y.Doc>]
        Auth[JWT Verifier]
    end

    Node1 <--> RedisPubSub[Redis Pub/Sub\n(跨节点广播)]
    Node2 <--> RedisPubSub

    Node1 -- Push Update --> RedisStream[Redis Stream\n(持久化缓冲)]
    Node2 -- Push Update --> RedisStream

    RedisStream --> JavaWorker[Sync-Worker (Java)]
    JavaWorker --> DB[(PostgreSQL)]
    JavaWorker --> MinIO[(MinIO)]
```

---

### 4. 核心模块详细设计
#### 4.1 连接层与鉴权 (Connection & Auth)
由于 WebSocket 建立后就是长连接，鉴权必须在握手阶段完成。

+ **握手 URL**: `ws://api.example.com/doc/ws/{docId}?token={JWT}`
+ **鉴权逻辑**:
    1. **解析 Token**: 使用与 Spring Boot 共享的 **RSA 公钥** 解密 JWT。
    2. **权限校验**: 检查 Token 中的 `scope` 或 `resource_ids` 是否包含当前 `docId`。（或者简单点，只校验身份，建立连接后通过 HTTP 回调 Spring Boot 校验权限，但这会增加延迟，推荐 JWT 携带权限）。
    3. **只读/读写**: 根据权限设置 Yjs 客户端的只读标记。

#### 4.2 内存状态管理 (In-Memory State)
这是 Node.js 的核心。为了速度，文档状态是加载到内存中的。

+ **数据结构**: `Map<String, WSSharedDoc>`
    - Key: `doc_id`
    - Value: `WSSharedDoc` (封装了 `Y.Doc` 实例 + 连接到该文档的所有 `WebSocket` 客户端列表)。
+ **生命周期**:
    - **Load**: 第一个人进入房间时，Node.js 尝试从 Redis Stream 或通过 HTTP 请求 Java 后端获取文档的初始状态（Binary Snapshot）。
    - **Active**: 只要有人在房间，`Y.Doc` 就常驻内存。
    - **Unload**: 房间内最后一个人离开后，延迟 N 秒（如 30s）销毁内存对象，释放内存。

#### 4.3 广播机制 (Broadcasting)
如何处理多节点扩展（Scale-out）？如果用户 A 连到了 Node 1，用户 B 连到了 Node 2，他们如何协同？

**答案：Redis Pub/Sub**

1. **本地广播**: 用户 A 发送 Update -> Node 1 接收 -> Node 1 更新本地 `Y.Doc` -> Node 1 广播给连接在自己身上的用户 C, D。
2. **跨节点广播**: Node 1 将 Update 封装，Publish 到 Redis Channel `yjs:pubsub:{docId}`。
3. **接收**: Node 2 订阅了该 Channel -> 收到 Update -> 更新 Node 2 本地的 `Y.Doc` -> 广播给用户 B。

#### 4.4 持久化管道 (Persistence Pipeline)
Node.js **绝不**直接写 PostgreSQL。它只负责“快写”。

+ **写入策略**:
    - 每收到前端的一个 Update（哪怕只有一个字节的变动），Node.js 都会将其 Push 到 **Redis Stream**。
    - **Key**: `yjs:updates:stream`
    - **Value**: `{ doc_id: "1001", update: <Buffer ...>, ts: 17100000 }`
+ **Java Worker 的职责** (在 `sync-worker` 服务中):
    - 作为 Consumer Group 消费 Stream。
    - **去抖动 (Debounce)**: 对同一个 `doc_id` 的更新，在内存中缓存 5秒 或 积攒 100条。
    - **合并 (Merge)**: 使用 `Y.mergeUpdates([update1, update2...])` 合并成一个大的 Update。
    - **落库**: 读取 Postgres 中的 `snapshot`，合并新的 Update，写回 Postgres。

---

### 5. 核心代码逻辑 (Node.js)
以下代码基于 `y-websocket` 的高度定制版逻辑。

#### 5.1 服务入口 (`server.js`)
```javascript
const WebSocket = require('ws');
const http = require('http');
const jwt = require('jsonwebtoken');
const { setupWSConnection } = require('./utils.js');

const wss = new WebSocket.Server({ noServer: true });
const PUBLIC_KEY = process.env.JWT_PUBLIC_KEY; // 与 Spring Boot 共享的公钥

const server = http.createServer((request, response) => {
  response.writeHead(200, { 'Content-Type': 'text/plain' });
  response.end('Collab Service is Running');
});

server.on('upgrade', (request, socket, head) => {
  const url = new URL(request.url, 'http://localhost');
  const token = url.searchParams.get('token');
  const docId = url.pathname.split('/').pop(); // /doc/ws/123 -> 123

  // 1. JWT 本地鉴权 (极速)
  try {
    const decoded = jwt.verify(token, PUBLIC_KEY, { algorithms: ['RS256'] });
    // 可以在这里检查 decoded.uid 是否有权访问 docId
    
    // 2. 升级连接
    wss.handleUpgrade(request, socket, head, (ws) => {
      setupWSConnection(ws, request, docId, decoded.uid);
    });
  } catch (err) {
    socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
    socket.destroy();
  }
});

server.listen(3000);
```

#### 5.2 核心协同逻辑 (`utils.js`)
```javascript
const Y = require('yjs');
const syncProtocol = require('y-protocols/dist/sync.cjs');
const awarenessProtocol = require('y-protocols/dist/awareness.cjs');
const Redis = require('ioredis');

// 两个 Redis 链接：一个用于发布/写入，一个用于订阅 (因为订阅是阻塞连接)
const redisPub = new Redis(process.env.REDIS_URL);
const redisSub = new Redis(process.env.REDIS_URL);

// 内存文档缓存
const docs = new Map();

class WSSharedDoc {
  constructor(docId) {
    this.docId = docId;
    this.ydoc = new Y.Doc();
    this.conns = new Map(); // Set<WebSocket>
    this.awareness = new awarenessProtocol.Awareness(this.ydoc);
    
    // 绑定 Update 事件
    this.ydoc.on('update', this.onUpdate.bind(this));
    
    // 订阅 Redis 跨节点消息
    redisSub.subscribe(`yjs:pubsub:${docId}`);
    redisSub.on('message', (channel, message) => {
       if (channel === `yjs:pubsub:${docId}`) {
           // 应用来自其他节点的更新
           const update = Buffer.from(message, 'base64');
           Y.applyUpdate(this.ydoc, update, 'redis'); // origin='redis' 避免死循环
       }
    });
    
    // 初始化加载 (略：从后端 API 拉取 Snapshot)
  }

  // 当文档发生变化 (无论是来自前端，还是来自 Redis)
  onUpdate(update, origin) {
    // 1. 广播给连接到当前节点的所有前端
    const encoder = syncProtocol.createUpdateMessage(update);
    this.conns.forEach((_, conn) => send(conn, encoder));

    // 2. 如果是来自前端的变化 (origin !== 'redis')，需要广播给其他节点并持久化
    if (origin !== 'redis') {
        // Pub/Sub 广播
        redisPub.publish(`yjs:pubsub:${this.docId}`, Buffer.from(update).toString('base64'));
        
        // Stream 持久化 (发送给 Java Worker)
        redisPub.xadd('yjs:stream', '*', 'docId', this.docId, 'update', Buffer.from(update));
    }
  }
}

// 建立连接逻辑
exports.setupWSConnection = (conn, req, docId, userId) => {
  let doc = docs.get(docId);
  if (!doc) {
      doc = new WSSharedDoc(docId);
      docs.set(docId, doc);
  }
  
  doc.conns.set(conn, new Set());
  
  // 监听前端消息
  conn.on('message', (message) => {
      // Yjs 协议处理：SyncStep1, SyncStep2, Update, Awareness
      // 使用 y-protocols 库处理 buffer
      // ...
  });

  conn.on('close', () => {
      doc.conns.delete(conn);
      if (doc.conns.size === 0) {
          // 延迟销毁 docs.delete(docId)...
      }
  });
};
```

---

### 6. 光标与感知 (Awareness)
协同编辑不仅是看内容，还要看到“谁在哪里”。

+ **Awareness Protocol**: Yjs 自带一套极其高效的 Awareness 协议。
+ **数据结构**: 临时的 JSON 对象，如 `{ "user": { "name": "Dev", "color": "#ff0000" }, "cursor": { "anchor": 10, "head": 15 } }`。
+ **传播**: Awareness 信息**不写入数据库**，也不写入 Redis Stream。它只通过 WebSocket 在用户之间和 Node.js 节点之间（通过 Redis Pub/Sub）实时广播。用户掉线后，Awareness 状态自动在其他客户端消失。

---

### 7. 异常处理与边缘情况
#### 7.1 Node.js 宕机
如果 `Node 1` 崩溃了，内存中的 `Y.Doc` 丢失了怎么办？

1. **前端重连**: 客户端会自动重连到 `Node 2`。
2. **状态恢复**: 客户端本地有完整的 `Y.Doc` 副本。
3. **同步**: 客户端向 `Node 2` 发送 `SyncStep1`，将本地所有未同步的 Update 推送给 `Node 2`。
4. **结果**: 零数据丢失，用户仅感觉到短暂的网络卡顿。

#### 7.2 Redis Stream 积压
如果 Java Worker 挂了，Redis Stream 会无限增长。

+ **监控**: 监控 Stream 的 `XLEN`，如果超过阈值报警。
+ **策略**: Redis Stream 仅作为缓冲，设置 `MAXLEN` (如 100万)。如果真的溢出，最坏情况是丢了“中间过程”，但只要最终状态一致即可（Yjs 保证最终一致性）。

---

### 8. 总结
`collab-service` 的设计精髓在于 **“快慢分离”**：

+ **快**: Node.js + Redis Pub/Sub 处理毫秒级的协同和广播。
+ **慢**: Java Worker + Redis Stream + Postgres 处理秒级的持久化。

这种架构不仅性能极致，而且与你的 Spring Boot 微服务体系完美融合，通过 Redis 实现了无缝对接。

