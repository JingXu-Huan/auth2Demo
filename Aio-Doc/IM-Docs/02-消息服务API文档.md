# 消息服务 API 文档

## 服务信息
- **服务名称**: message-service  
- **端口**: 8002  
- **基础路径**: /api/v1/messages  
- **版本**: v1.0.0

---

## 目录
1. [发送单聊消息](#1-发送单聊消息)
2. [发送群聊消息](#2-发送群聊消息)
3. [发送图片消息](#3-发送图片消息)
4. [获取历史消息](#4-获取历史消息)
5. [撤回消息](#5-撤回消息)
6. [标记消息已读](#6-标记消息已读)
7. [获取未读消息数](#7-获取未读消息数)
8. [转发消息](#8-转发消息)

---

## 1. 发送单聊消息

### 接口信息
- **URL**: `/private`
- **Method**: `POST`
- **功能**: 发送一对一私聊消息
- **认证**: 需要 Bearer Token

### Request
```json
{
  "receiverId": 10002,
  "messageType": "TEXT",
  "content": "你好，在吗？",
  "clientMsgId": "client_msg_123456"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| receiverId | number | 是 | 接收者用户ID |
| messageType | string | 是 | 消息类型：TEXT/IMAGE/VIDEO/AUDIO/FILE |
| content | string/object | 是 | 消息内容，文本或JSON对象 |
| clientMsgId | string | 是 | 客户端消息ID，用于去重 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "发送成功",
  "data": {
    "messageId": "msg_789012",
    "senderId": 10001,
    "receiverId": 10002,
    "messageType": "TEXT",
    "content": "你好，在吗？",
    "status": "SENT",
    "timestamp": "2025-11-09T15:30:00Z",
    "clientMsgId": "client_msg_123456"
  }
}
```

---

## 2. 发送群聊消息

### 接口信息
- **URL**: `/group`
- **Method**: `POST`
- **功能**: 发送群组消息
- **认证**: 需要 Bearer Token

### Request
```json
{
  "groupId": "group_001",
  "messageType": "TEXT",
  "content": "大家好！",
  "atUserIds": [10002, 10003],
  "clientMsgId": "client_msg_123457"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| groupId | string | 是 | 群组ID |
| messageType | string | 是 | 消息类型 |
| content | string/object | 是 | 消息内容 |
| atUserIds | array | 否 | @提醒的用户ID列表 |
| clientMsgId | string | 是 | 客户端消息ID |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "发送成功",
  "data": {
    "messageId": "msg_789013",
    "senderId": 10001,
    "groupId": "group_001",
    "messageType": "TEXT",
    "content": "大家好！",
    "atUserIds": [10002, 10003],
    "status": "SENT",
    "timestamp": "2025-11-09T15:31:00Z"
  }
}
```

---

## 3. 发送图片消息

### 接口信息
- **URL**: `/image`
- **Method**: `POST`
- **功能**: 发送图片消息
- **认证**: 需要 Bearer Token

### Request
```json
{
  "receiverId": 10002,
  "messageType": "IMAGE",
  "content": {
    "url": "https://cdn.example.com/images/abc123.jpg",
    "thumbnail": "https://cdn.example.com/images/abc123_thumb.jpg",
    "width": 1920,
    "height": 1080,
    "size": 524288,
    "format": "jpg"
  },
  "clientMsgId": "client_msg_123458"
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "发送成功",
  "data": {
    "messageId": "msg_789014",
    "senderId": 10001,
    "receiverId": 10002,
    "messageType": "IMAGE",
    "content": {
      "url": "https://cdn.example.com/images/abc123.jpg",
      "thumbnail": "https://cdn.example.com/images/abc123_thumb.jpg",
      "width": 1920,
      "height": 1080,
      "size": 524288
    },
    "timestamp": "2025-11-09T15:32:00Z"
  }
}
```

---

## 4. 获取历史消息

### 接口信息
- **URL**: `/history`
- **Method**: `GET`
- **功能**: 获取与指定用户或群组的聊天历史
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/messages/history?userId=10002&before=msg_789014&limit=20
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | number | 否 | 用户ID（单聊） |
| groupId | string | 否 | 群组ID（群聊） |
| before | string | 否 | 消息ID，获取此消息之前的历史 |
| limit | integer | 否 | 数量限制，默认20，最大100 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "hasMore": true,
    "messages": [
      {
        "messageId": "msg_789014",
        "senderId": 10001,
        "receiverId": 10002,
        "messageType": "TEXT",
        "content": "你好",
        "status": "READ",
        "timestamp": "2025-11-09T15:30:00Z"
      }
    ]
  }
}
```

---

## 5. 撤回消息

### 接口信息
- **URL**: `/{messageId}/recall`
- **Method**: `POST`
- **功能**: 撤回已发送的消息（2分钟内）
- **认证**: 需要 Bearer Token

### Request
```json
{
  "reason": "发错了"
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "撤回成功",
  "data": {
    "messageId": "msg_789014",
    "status": "RECALLED",
    "recalledAt": "2025-11-09T15:32:00Z"
  }
}
```

### Response - 失败
```json
{
  "code": 400,
  "message": "超过2分钟，无法撤回",
  "data": null
}
```

---

## 6. 标记消息已读

### 接口信息
- **URL**: `/read`
- **Method**: `POST`
- **功能**: 标记消息为已读状态
- **认证**: 需要 Bearer Token

### Request
```json
{
  "messageIds": ["msg_789014", "msg_789013", "msg_789012"]
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "已标记为已读",
  "data": {
    "readCount": 3,
    "readAt": "2025-11-09T15:33:00Z"
  }
}
```

---

## 7. 获取未读消息数

### 接口信息
- **URL**: `/unread/count`
- **Method**: `GET`
- **功能**: 获取未读消息总数和会话列表
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/messages/unread/count
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUnread": 15,
    "conversations": [
      {
        "userId": 10002,
        "unreadCount": 5,
        "lastMessage": {
          "messageId": "msg_789014",
          "content": "你好",
          "timestamp": "2025-11-09T15:30:00Z"
        }
      },
      {
        "groupId": "group_001",
        "unreadCount": 10,
        "lastMessage": {
          "messageId": "msg_789015",
          "content": "大家好",
          "timestamp": "2025-11-09T15:31:00Z"
        }
      }
    ]
  }
}
```

---

## 8. 转发消息

### 接口信息
- **URL**: `/forward`
- **Method**: `POST`
- **功能**: 转发消息到其他用户或群组
- **认证**: 需要 Bearer Token

### Request
```json
{
  "messageId": "msg_789014",
  "targetUserIds": [10003, 10004],
  "targetGroupIds": ["group_002"]
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "转发成功",
  "data": {
    "forwardedCount": 3,
    "newMessageIds": ["msg_789020", "msg_789021", "msg_789022"]
  }
}
```

---

## 消息类型

| 类型 | 说明 | Content格式 |
|------|------|-------------|
| TEXT | 文本消息 | string |
| IMAGE | 图片消息 | {url, thumbnail, width, height, size} |
| VIDEO | 视频消息 | {url, thumbnail, duration, size} |
| AUDIO | 语音消息 | {url, duration, size} |
| FILE | 文件消息 | {url, filename, size, format} |
| LOCATION | 位置消息 | {latitude, longitude, address} |
| CARD | 名片消息 | {userId, username, avatar} |
| SYSTEM | 系统消息 | string |

---

## 消息状态

| 状态 | 说明 |
|------|------|
| SENDING | 发送中 |
| SENT | 已发送 |
| DELIVERED | 已送达 |
| READ | 已读 |
| FAILED | 发送失败 |
| RECALLED | 已撤回 |

---

## 数据模型

### Message
```typescript
interface Message {
  messageId: string;          // 消息ID
  senderId: number;           // 发送者ID
  receiverId?: number;        // 接收者ID（单聊）
  groupId?: string;           // 群组ID（群聊）
  messageType: MessageType;   // 消息类型
  content: string | object;   // 消息内容
  status: MessageStatus;      // 消息状态
  timestamp: string;          // 发送时间
  clientMsgId: string;        // 客户端消息ID
  atUserIds?: number[];       // @的用户ID列表
  replyTo?: string;           // 回复的消息ID
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-09  
**维护人**: 开发团队
