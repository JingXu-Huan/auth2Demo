const WebSocket = require('ws');
const Y = require('yjs');
const Redis = require('ioredis');

// Redis 连接（一个用于发布/写入，一个用于订阅）
const redisUrl = process.env.REDIS_URL || 'redis://127.0.0.1:6379';
const redisPub = new Redis(redisUrl);
const redisSub = new Redis(redisUrl);

const STREAM_KEY = 'yjs:stream';
const CHANNEL_PREFIX = 'yjs:pubsub:';

// docId -> WSSharedDoc
const docs = new Map();

class WSSharedDoc {
  constructor(docId) {
    this.docId = docId;
    this.ydoc = new Y.Doc();
    this.conns = new Set(); // Set<WebSocket>

    // 当文档发生变更时（本地或远端），触发更新广播与持久化
    this.ydoc.on('update', (update, origin) => {
      this.onUpdate(update, origin);
    });
  }

  onUpdate(update, origin) {
    // 1. 广播给连接在当前节点上的所有客户端（Yjs update 二进制）
    for (const ws of this.conns) {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(update);
      }
    }

    // 2. 来自本节点（非 redis）的更新，需要：
    //    - 通过 Redis Pub/Sub 广播到其他节点
    //    - 写入 Redis Stream，供 Java sync-worker 消费
    if (origin !== 'redis') {
      const base64 = Buffer.from(update).toString('base64');
      const channel = CHANNEL_PREFIX + this.docId;

      // 跨节点广播
      redisPub.publish(channel, base64).catch((err) => {
        console.error('Redis publish error', err);
      });

      // 写入 Stream 供 Java Worker 持久化
      redisPub.xadd(STREAM_KEY, '*', 'docId', this.docId, 'update', base64)
        .catch((err) => {
          console.error('Redis XADD error', err);
        });
    }
  }

  addConnection(ws) {
    this.conns.add(ws);
  }

  removeConnection(ws) {
    this.conns.delete(ws);
  }
}

// 统一处理 Redis 订阅消息：将来自其他节点的 update 应用到本地 Y.Doc
redisSub.on('message', (channel, message) => {
  if (!channel.startsWith(CHANNEL_PREFIX)) return;
  const docId = channel.substring(CHANNEL_PREFIX.length);
  const doc = docs.get(docId);
  if (!doc) return;

  try {
    const update = Buffer.from(message, 'base64');
    Y.applyUpdate(doc.ydoc, update, 'redis');
  } catch (err) {
    console.error('Failed to apply update from Redis', err);
  }
});

// 可选：从后端加载初始快照（Binary Yjs Update）
async function loadInitialSnapshot(doc) {
  const baseUrl = process.env.SNAPSHOT_BASE_URL;
  if (!baseUrl) return;

  const url = `${baseUrl.replace(/\/$/, '')}/${encodeURIComponent(doc.docId)}`;
  try {
    const res = await fetch(url);
    if (!res.ok) {
      console.error('Snapshot fetch failed', res.status, url);
      return;
    }
    const buf = await res.arrayBuffer();
    if (!buf.byteLength) return;

    const update = new Uint8Array(buf);
    Y.applyUpdate(doc.ydoc, update, 'snapshot');
  } catch (err) {
    console.error('Snapshot load error', err);
  }
}

function getOrCreateDoc(docId) {
  let doc = docs.get(docId);
  if (!doc) {
    doc = new WSSharedDoc(docId);
    docs.set(docId, doc);
    // 订阅该文档的跨节点更新
    const channel = CHANNEL_PREFIX + docId;
    redisSub.subscribe(channel).catch((err) => {
      console.error('Redis subscribe error', err);
    });

    // 异步加载初始快照（如果配置了 SNAPHOT_BASE_URL）
    // 不阻塞初次连接建立，加载完成后会通过 Y.update 触发广播
    loadInitialSnapshot(doc).catch((err) => {
      console.error('Snapshot init failed', err);
    });
  }
  return doc;
}

// 简单的只读权限判定示例：可以按需要在 JWT 中扩展字段
function isReadOnly(decodedToken, docId) {
  if (!decodedToken) return false;

  // 显式只读标记
  if (decodedToken.collabRole === 'read_only' || decodedToken.readonly === true) {
    return true;
  }

  // scopes 中只有读没有写，则视为只读（示例约定，可根据自己JWT结构调整）
  const scopes = decodedToken.scopes || decodedToken.scope;
  if (Array.isArray(scopes)) {
    const hasWrite = scopes.some((s) => typeof s === 'string' && s.includes('doc:write'));
    const hasRead = scopes.some((s) => typeof s === 'string' && s.includes('doc:read'));
    if (hasRead && !hasWrite) {
      return true;
    }
  }

  return false;
}

// 建立 WebSocket 连接，绑定到指定文档
function setupWSConnection(conn, req, docId, userId, decodedToken) {
  const doc = getOrCreateDoc(docId);
  doc.addConnection(conn);

  const readOnly = isReadOnly(decodedToken, docId);
  conn._readOnly = readOnly;

  conn.on('message', (message) => {
    try {
      // 字符串消息：用于传输 awareness 等非持久化信息
      if (typeof message === 'string') {
        let payload;
        try {
          payload = JSON.parse(message);
        } catch (e) {
          console.error('Invalid JSON message', e);
          return;
        }
        if (payload && payload.type === 'awareness') {
          // Awareness 仅在本节点内广播，不写入 Redis / Stream
          for (const ws of doc.conns) {
            if (ws !== conn && ws.readyState === WebSocket.OPEN) {
              ws.send(message);
            }
          }
        }
        return;
      }

      // 二进制消息：约定为 Yjs update
      const update = Buffer.isBuffer(message)
        ? new Uint8Array(message)
        : new Uint8Array(Buffer.from(message));

      if (conn._readOnly) {
        // 只读用户不允许提交更新
        console.warn('Readonly client tried to send update, docId=%s, userId=%s', docId, userId);
        return;
      }

      Y.applyUpdate(doc.ydoc, update);
    } catch (err) {
      console.error('Failed to handle WS message', err);
    }
  });

  conn.on('close', () => {
    doc.removeConnection(conn);

    // 如果没有连接了，延迟销毁，释放内存
    if (doc.conns.size === 0) {
      setTimeout(() => {
        const current = docs.get(docId);
        if (current && current.conns.size === 0) {
          current.ydoc.destroy();
          docs.delete(docId);
        }
      }, Number(process.env.DOC_UNLOAD_DELAY_MS || 30000));
    }
  });

  conn.on('error', (err) => {
    console.error('WebSocket error', err);
  });
}

module.exports = {
  setupWSConnection,
};
