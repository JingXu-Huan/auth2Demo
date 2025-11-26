const fastify = require('fastify')({ logger: true });
const WebSocket = require('ws');
const jwt = require('jsonwebtoken');
const { URL } = require('url');
const { setupWSConnection } = require('./utils');

// 与 Spring Boot 共享的 JWT 公钥（RS256）
const PUBLIC_KEY = process.env.JWT_PUBLIC_KEY;

// 使用 Fastify 提供 HTTP 能力，复用其底层 server 处理 WebSocket 升级
const wss = new WebSocket.Server({ noServer: true });

fastify.get('/health', async () => {
  return { status: 'UP', service: 'collab-service' };
});

// 处理 WebSocket 升级：ws://host/doc/ws/{docId}?token=JWT
fastify.server.on('upgrade', (request, socket, head) => {
  try {
    const url = new URL(request.url, 'http://localhost');
    const token = url.searchParams.get('token');
    const segments = url.pathname.split('/').filter(Boolean); // e.g. ["doc","ws","123"]
    const docId = segments[segments.length - 1];

    if (!docId) {
      socket.write('HTTP/1.1 400 Bad Request\r\n\r\n');
      socket.destroy();
      return;
    }

    if (!token || !PUBLIC_KEY) {
      socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
      socket.destroy();
      return;
    }

    let decoded;
    try {
      decoded = jwt.verify(token, PUBLIC_KEY, { algorithms: ['RS256'] });
    } catch (err) {
      fastify.log.warn({ err }, 'JWT verify failed');
      socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
      socket.destroy();
      return;
    }

    const userId = decoded.user_id || decoded.sub || decoded.uid;

    wss.handleUpgrade(request, socket, head, (ws) => {
      setupWSConnection(ws, request, docId, userId, decoded);
    });
  } catch (err) {
    fastify.log.error({ err }, 'WebSocket upgrade error');
    socket.write('HTTP/1.1 500 Internal Server Error\r\n\r\n');
    socket.destroy();
  }
});

const port = Number(process.env.PORT || 3001);

fastify.listen({ port, host: '0.0.0.0' }).then(() => {
  fastify.log.info(`collab-service listening on 0.0.0.0:${port}`);
}).catch((err) => {
  fastify.log.error(err, 'Failed to start collab-service');
  process.exit(1);
});
