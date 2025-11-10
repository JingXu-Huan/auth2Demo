# WebSocket 协议文档

## 服务信息
- **协议**: WebSocket + STOMP
- **端点**: ws://localhost:8080/ws
- **传输层**: SockJS
- **消息协议**: STOMP 1.2

---

## 目录
1. [连接建立](#1-连接建立)
2. [订阅主题](#2-订阅主题)
3. [发送消息](#3-发送消息)
4. [接收消息](#4-接收消息)
5. [心跳机制](#5-心跳机制)
6. [错误处理](#6-错误处理)
7. [断开连接](#7-断开连接)

---

## 1. 连接建立

### 连接流程

```
客户端                    服务端
   │                         │
   ├── HTTP Upgrade ──────► │
   │                         │
   │ ◄─── 101 Switching ───├
   │                         │
   ├── STOMP CONNECT ──────► │
   │                         │
   │ ◄─── STOMP CONNECTED ─├
   │                         │
   └───── 连接建立成功 ─────┘
```

### JavaScript 客户端示例

```javascript
import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'

// 1. 创建SockJS连接
const socket = new SockJS('http://localhost:8080/ws')

// 2. 创建STOMP客户端
const stompClient = Stomp.over(socket)

// 3. 连接到服务器
stompClient.connect(
  {
    // 连接头信息
    username: 'user123',
    token: 'Bearer eyJhbGci...'
  },
  // 连接成功回调
  (frame) => {
    console.log('Connected:', frame)
    // 开始订阅主题
  },
  // 连接失败回调
  (error) => {
    console.error('Connection error:', error)
  }
)
```

### STOMP CONNECT 帧

```
CONNECT
accept-version:1.2
heart-beat:10000,10000
username:user123
token:Bearer eyJhbGci...

^@
```

### STOMP CONNECTED 帧

```
CONNECTED
version:1.2
heart-beat:10000,10000
session:session-abc123

^@
```

---

## 2. 订阅主题

### 主题列表

| 主题 | 类型 | 说明 |
|------|------|------|
| `/topic/private/{username}` | 点对点 | 个人私聊消息 |
| `/topic/group/{groupId}` | 广播 | 群组消息 |
| `/topic/system` | 广播 | 系统通知 |
| `/user/queue/notifications` | 个人 | 个人通知 |

### 订阅示例

```javascript
// 订阅私聊消息
const subscription1 = stompClient.subscribe(
  '/topic/private/user123',
  (message) => {
    const msg = JSON.parse(message.body)
    console.log('收到私聊消息:', msg)
  }
)

// 订阅群组消息
const subscription2 = stompClient.subscribe(
  '/topic/group/group_001',
  (message) => {
    const msg = JSON.parse(message.body)
    console.log('收到群聊消息:', msg)
  }
)

// 订阅系统消息
const subscription3 = stompClient.subscribe(
  '/topic/system',
  (message) => {
    const msg = JSON.parse(message.body)
    console.log('系统通知:', msg)
  }
)
```

### STOMP SUBSCRIBE 帧

```
SUBSCRIBE
id:sub-0
destination:/topic/private/user123
ack:auto

^@
```

---

## 3. 发送消息

### 发送目标

| 目标 | 说明 |
|------|------|
| `/app/chat/private` | 发送私聊消息 |
| `/app/chat/group` | 发送群聊消息 |
| `/app/chat/system` | 发送系统消息 |
| `/app/presence/heartbeat` | 发送心跳 |

### 发送私聊消息

```javascript
const message = {
  senderId: 'user123',
  receiverId: 'user456',
  messageType: 'TEXT',
  content: '你好！',
  timestamp: Date.now()
}

stompClient.send(
  '/app/chat/private',
  {},
  JSON.stringify(message)
)
```

### 发送群聊消息

```javascript
const groupMessage = {
  senderId: 'user123',
  groupId: 'group_001',
  messageType: 'TEXT',
  content: '大家好！',
  atUserIds: ['user456', 'user789'],
  timestamp: Date.now()
}

stompClient.send(
  '/app/chat/group',
  {},
  JSON.stringify(groupMessage)
)
```

### STOMP SEND 帧

```
SEND
destination:/app/chat/private
content-type:application/json

{"senderId":"user123","receiverId":"user456","content":"你好"}
^@
```

---

## 4. 接收消息

### 消息格式

#### 私聊消息

```json
{
  "messageId": "msg_123456",
  "senderId": "user456",
  "receiverId": "user123",
  "messageType": "TEXT",
  "content": "你好！",
  "timestamp": 1699520400000,
  "status": "SENT"
}
```

#### 群聊消息

```json
{
  "messageId": "msg_123457",
  "senderId": "user456",
  "groupId": "group_001",
  "messageType": "TEXT",
  "content": "大家好！",
  "atUserIds": ["user123"],
  "timestamp": 1699520400000,
  "status": "SENT"
}
```

#### 系统消息

```json
{
  "type": "SYSTEM",
  "senderId": "user456",
  "content": "online",
  "timestamp": 1699520400000
}
```

### 处理消息示例

```javascript
stompClient.subscribe('/topic/private/user123', (message) => {
  const msg = JSON.parse(message.body)
  
  switch(msg.messageType) {
    case 'TEXT':
      displayTextMessage(msg)
      break
    case 'IMAGE':
      displayImageMessage(msg)
      break
    case 'VIDEO':
      displayVideoMessage(msg)
      break
    default:
      console.log('Unknown message type:', msg.messageType)
  }
  
  // 发送已读回执
if (msg.senderId !== currentUserId) {
    sendReadReceipt(msg.messageId)
  }
})
```

### STOMP MESSAGE 帧

```
MESSAGE
destination:/topic/private/user123
message-id:msg-123
subscription:sub-0
content-type:application/json

{"messageId":"msg_123456","senderId":"user456","content":"你好"}
^@
```

---

## 5. 心跳机制

### 心跳配置

```javascript
stompClient.heartbeat.outgoing = 10000  // 客户端发送心跳间隔（10秒）
stompClient.heartbeat.incoming = 10000  // 期望服务端心跳间隔（10秒）
```

### 心跳帧

```
\n  // 客户端发送的心跳
```

### 心跳超时处理

```javascript
stompClient.onWebSocketClose = (event) => {
  console.log('WebSocket closed:', event)
  // 尝试重连
  reconnect()
}

function reconnect() {
  setTimeout(() => {
    console.log('Attempting to reconnect...')
    stompClient.connect(...)
  }, 3000)
}
```

---

## 6. 错误处理

### 错误类型

| 错误码 | 说明 |
|--------|------|
| 401 | 未授权，Token无效 |
| 403 | 禁止访问 |
| 404 | 目标不存在 |
| 500 | 服务器错误 |

### STOMP ERROR 帧

```
ERROR
message:Unauthorized
content-type:text/plain

Authentication failed: Invalid token
^@
```

### 错误处理示例

```javascript
stompClient.onStompError = (frame) => {
  console.error('STOMP error:', frame.headers['message'])
  console.error('Error details:', frame.body)
  
  // 根据错误类型处理
  if (frame.headers['message'].includes('Unauthorized')) {
    // Token过期，重新登录
    redirectToLogin()
  }
}
```

---

## 7. 断开连接

### 主动断开

```javascript
// 取消订阅
subscription1.unsubscribe()
subscription2.unsubscribe()

// 断开连接
stompClient.disconnect(() => {
  console.log('Disconnected')
})
```

### STOMP DISCONNECT 帧

```
DISCONNECT
receipt:disconnect-123

^@
```

### STOMP RECEIPT 帧

```
RECEIPT
receipt-id:disconnect-123

^@
```

---

## 完整示例

### Vue 3 完整实现

```javascript
import { ref, onMounted, onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'

export default {
  setup() {
    const stompClient = ref(null)
    const connected = ref(false)
    const messages = ref([])
    
    const connect = () => {
      const socket = new SockJS('/ws')
      stompClient.value = Stomp.over(socket)
      
      stompClient.value.connect(
        { username: 'user123' },
        (frame) => {
          connected.value = true
          
          // 订阅私聊
          stompClient.value.subscribe(
            '/topic/private/user123',
            (message) => {
              messages.value.push(JSON.parse(message.body))
            }
          )
          
          // 订阅系统消息
          stompClient.value.subscribe(
            '/topic/system',
            (message) => {
              handleSystemMessage(JSON.parse(message.body))
            }
          )
        },
        (error) => {
          console.error('Connection error:', error)
          connected.value = false
        }
      )
    }
    
    const sendMessage = (content) => {
      if (!connected.value) return
      
      const message = {
        senderId: 'user123',
        receiverId: 'user456',
        messageType: 'TEXT',
        content: content,
        timestamp: Date.now()
      }
      
      stompClient.value.send(
        '/app/chat/private',
        {},
        JSON.stringify(message)
      )
    }
    
    const disconnect = () => {
      if (stompClient.value) {
        stompClient.value.disconnect()
        connected.value = false
      }
    }
    
    onMounted(() => {
      connect()
    })
    
    onUnmounted(() => {
      disconnect()
    })
    
    return {
      connected,
      messages,
      sendMessage
    }
  }
}
```

---

## 最佳实践

### 1. 连接管理

- ✅ 实现自动重连机制
- ✅ 设置合理的超时时间
- ✅ 处理网络断开场景

### 2. 消息处理

- ✅ 消息去重（使用clientMsgId）
- ✅ 消息排序（按timestamp）
- ✅ 离线消息同步

### 3. 性能优化

- ✅ 批量发送消息
- ✅ 压缩大消息
- ✅ 分页加载历史消息

### 4. 安全性

- ✅ 使用WSS（加密连接）
- ✅ Token认证
- ✅ 消息内容验证

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-09  
**维护人**: 开发团队
