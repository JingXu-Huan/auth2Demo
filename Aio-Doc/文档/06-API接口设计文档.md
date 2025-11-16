# API接口设计文档 (API Interface Design Document)

## 📋 文档信息

| 项目信息 | 详情 |
|---------|------|
| **项目名称** | AIO 企业级即时通讯系统 |
| **文档类型** | API接口设计文档 |
| **文档版本** | v1.0.0 |
| **创建日期** | 2025-11-12 |
| **设计师** | 后端开发团队 |

---

## 🎯 接口设计原则

### RESTful API设计规范
1. **资源导向**: 使用名词表示资源，动词表示操作
2. **HTTP方法**: 合理使用GET、POST、PUT、DELETE
3. **状态码**: 标准HTTP状态码表示操作结果
4. **版本控制**: 通过URL路径进行版本管理
5. **统一响应**: 标准化的响应格式

### 接口命名规范
- **URL路径**: 小写字母，单词间用连字符分隔
- **参数名**: 驼峰命名法
- **响应字段**: 驼峰命名法
- **错误码**: 数字编码 + 英文描述

---

## 🔗 接口概览

### 服务端口分配
| 服务名称 | 端口 | 基础路径 | 说明 |
|---------|------|---------|------|
| Gateway | 9000 | /api/v1 | API网关 |
| User-Service | 8001 | /api/v1/users | 用户服务 |
| Message-Service | 8002 | /api/v1/messages | 消息服务 |
| Group-Service | 8003 | /api/v1/groups | 群组服务 |
| File-Service | 8005 | /api/v1/files | 文件服务 |
| Search-Service | 8006 | /api/v1/search | 搜索服务 |
| Notification-Service | 8007 | /api/v1/notifications | 通知服务 |

### 通用响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2025-11-12T22:30:00Z",
  "traceId": "abc123def456"
}
```

---

## 👤 用户服务接口 (User Service)

### 1. 用户认证接口

#### 1.1 用户注册
**接口地址**: `POST /api/v1/users/register`

**请求参数**:
```json
{
  "username": "string",     // 用户名，3-20字符
  "password": "string",     // 密码，8-32字符
  "email": "string",        // 邮箱地址
  "phone": "string",        // 手机号码
  "nickname": "string",     // 昵称，可选
  "avatar": "string"        // 头像URL，可选
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 12345,
    "username": "testuser",
    "email": "test@example.com",
    "nickname": "测试用户",
    "avatar": "https://example.com/avatar.jpg",
    "status": "active",
    "createdAt": "2025-11-12T22:30:00Z"
  }
}
```

#### 1.2 用户登录
**接口地址**: `POST /api/v1/users/login`

**请求参数**:
```json
{
  "username": "string",     // 用户名或邮箱
  "password": "string",     // 密码
  "captcha": "string",      // 验证码，可选
  "rememberMe": boolean     // 记住登录状态
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200,
    "tokenType": "Bearer",
    "userInfo": {
      "userId": 12345,
      "username": "testuser",
      "nickname": "测试用户",
      "avatar": "https://example.com/avatar.jpg",
      "email": "test@example.com",
      "status": "online"
    }
  }
}
```

#### 1.3 刷新Token
**接口地址**: `POST /api/v1/users/refresh-token`

**请求头**:
```
Authorization: Bearer {refreshToken}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Token刷新成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  }
}
```

### 2. 用户信息管理

#### 2.1 获取用户信息
**接口地址**: `GET /api/v1/users/{userId}`

**路径参数**:
- `userId`: 用户ID

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "userId": 12345,
    "username": "testuser",
    "nickname": "测试用户",
    "avatar": "https://example.com/avatar.jpg",
    "email": "test@example.com",
    "phone": "13800138000",
    "department": "技术部",
    "position": "软件工程师",
    "status": "online",
    "lastLoginTime": "2025-11-12T22:30:00Z",
    "createdAt": "2025-11-01T10:00:00Z"
  }
}
```

#### 2.2 更新用户信息
**接口地址**: `PUT /api/v1/users/{userId}`

**请求参数**:
```json
{
  "nickname": "string",     // 昵称
  "avatar": "string",       // 头像URL
  "phone": "string",        // 手机号码
  "signature": "string"     // 个性签名
}
```

#### 2.3 修改密码
**接口地址**: `PUT /api/v1/users/{userId}/password`

**请求参数**:
```json
{
  "oldPassword": "string",  // 原密码
  "newPassword": "string"   // 新密码
}
```

---

## 💬 消息服务接口 (Message Service)

### 1. 消息发送接口

#### 1.1 发送单聊消息
**接口地址**: `POST /api/v1/messages/private`

**请求参数**:
```json
{
  "receiverId": 67890,      // 接收者ID
  "messageType": "text",    // 消息类型: text/image/file/audio/video
  "content": "Hello World", // 消息内容
  "fileUrl": "string",      // 文件URL，可选
  "fileName": "string",     // 文件名，可选
  "fileSize": 1024,         // 文件大小，可选
  "replyToId": 123          // 回复消息ID，可选
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "消息发送成功",
  "data": {
    "messageId": "msg_123456789",
    "senderId": 12345,
    "receiverId": 67890,
    "messageType": "text",
    "content": "Hello World",
    "status": "sent",
    "timestamp": "2025-11-12T22:30:00Z"
  }
}
```

#### 1.2 发送群聊消息
**接口地址**: `POST /api/v1/messages/group`

**请求参数**:
```json
{
  "groupId": 98765,         // 群组ID
  "messageType": "text",    // 消息类型
  "content": "Hello Group", // 消息内容
  "mentionUsers": [67890],  // @用户列表，可选
  "replyToId": 123          // 回复消息ID，可选
}
```

### 2. 消息查询接口

#### 2.1 获取聊天历史
**接口地址**: `GET /api/v1/messages/history`

**查询参数**:
- `chatType`: 聊天类型 (private/group)
- `chatId`: 聊天对象ID
- `page`: 页码，默认1
- `size`: 每页大小，默认20
- `beforeTime`: 时间戳，获取此时间之前的消息

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "messages": [
      {
        "messageId": "msg_123456789",
        "senderId": 12345,
        "senderName": "张三",
        "senderAvatar": "https://example.com/avatar1.jpg",
        "messageType": "text",
        "content": "Hello World",
        "timestamp": "2025-11-12T22:30:00Z",
        "status": "read"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "hasMore": true
    }
  }
}
```

#### 2.2 标记消息已读
**接口地址**: `PUT /api/v1/messages/{messageId}/read`

**响应示例**:
```json
{
  "code": 200,
  "message": "标记成功"
}
```

---

## 👥 群组服务接口 (Group Service)

### 1. 群组管理接口

#### 1.1 创建群组
**接口地址**: `POST /api/v1/groups`

**请求参数**:
```json
{
  "groupName": "string",    // 群组名称
  "description": "string",  // 群组描述
  "avatar": "string",       // 群组头像
  "isPublic": true,         // 是否公开群组
  "maxMembers": 500,        // 最大成员数
  "memberIds": [67890, 11111] // 初始成员ID列表
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "群组创建成功",
  "data": {
    "groupId": 98765,
    "groupName": "技术交流群",
    "description": "技术讨论和分享",
    "avatar": "https://example.com/group-avatar.jpg",
    "ownerId": 12345,
    "memberCount": 3,
    "createdAt": "2025-11-12T22:30:00Z"
  }
}
```

#### 1.2 获取群组信息
**接口地址**: `GET /api/v1/groups/{groupId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "groupId": 98765,
    "groupName": "技术交流群",
    "description": "技术讨论和分享",
    "avatar": "https://example.com/group-avatar.jpg",
    "ownerId": 12345,
    "ownerName": "张三",
    "memberCount": 50,
    "maxMembers": 500,
    "isPublic": true,
    "createdAt": "2025-11-01T10:00:00Z",
    "announcement": "欢迎大家积极讨论技术问题"
  }
}
```

#### 1.3 获取群组成员列表
**接口地址**: `GET /api/v1/groups/{groupId}/members`

**查询参数**:
- `page`: 页码，默认1
- `size`: 每页大小，默认20
- `role`: 角色筛选 (owner/admin/member)

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "members": [
      {
        "userId": 12345,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": "https://example.com/avatar1.jpg",
        "role": "owner",
        "joinTime": "2025-11-01T10:00:00Z",
        "lastActiveTime": "2025-11-12T22:30:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 50,
      "hasMore": true
    }
  }
}
```

---

## 📁 文件服务接口 (File Service)

### 1. 文件上传接口

#### 1.1 上传文件
**接口地址**: `POST /api/v1/files/upload`

**请求类型**: `multipart/form-data`

**请求参数**:
- `file`: 文件对象
- `fileType`: 文件类型 (image/document/audio/video)
- `chatType`: 聊天类型 (private/group)
- `chatId`: 聊天对象ID

**响应示例**:
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "fileId": "file_123456789",
    "fileName": "document.pdf",
    "fileSize": 2048576,
    "fileType": "document",
    "mimeType": "application/pdf",
    "fileUrl": "https://cdn.example.com/files/document.pdf",
    "thumbnailUrl": "https://cdn.example.com/thumbnails/document.jpg",
    "uploadTime": "2025-11-12T22:30:00Z"
  }
}
```

#### 1.2 获取文件信息
**接口地址**: `GET /api/v1/files/{fileId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "fileId": "file_123456789",
    "fileName": "document.pdf",
    "fileSize": 2048576,
    "fileType": "document",
    "mimeType": "application/pdf",
    "fileUrl": "https://cdn.example.com/files/document.pdf",
    "uploaderId": 12345,
    "uploaderName": "张三",
    "uploadTime": "2025-11-12T22:30:00Z",
    "downloadCount": 5
  }
}
```

---

## 🔍 搜索服务接口 (Search Service)

### 1. 全局搜索接口

#### 1.1 搜索消息
**接口地址**: `GET /api/v1/search/messages`

**查询参数**:
- `keyword`: 搜索关键词
- `chatType`: 聊天类型 (private/group/all)
- `chatId`: 聊天对象ID，可选
- `messageType`: 消息类型筛选
- `startTime`: 开始时间
- `endTime`: 结束时间
- `page`: 页码，默认1
- `size`: 每页大小，默认10

**响应示例**:
```json
{
  "code": 200,
  "message": "搜索成功",
  "data": {
    "results": [
      {
        "messageId": "msg_123456789",
        "senderId": 12345,
        "senderName": "张三",
        "chatType": "group",
        "chatId": 98765,
        "chatName": "技术交流群",
        "content": "这是一条包含关键词的消息",
        "highlightContent": "这是一条包含<em>关键词</em>的消息",
        "timestamp": "2025-11-12T22:30:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 10,
      "total": 25,
      "hasMore": true
    }
  }
}
```

---

## 🔔 通知服务接口 (Notification Service)

### 1. 消息推送接口

#### 1.1 获取通知列表
**接口地址**: `GET /api/v1/notifications`

**查询参数**:
- `type`: 通知类型 (message/system/friend_request)
- `status`: 状态筛选 (unread/read/all)
- `page`: 页码，默认1
- `size`: 每页大小，默认20

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "notifications": [
      {
        "notificationId": "notif_123456789",
        "type": "message",
        "title": "新消息",
        "content": "张三给你发送了一条消息",
        "data": {
          "messageId": "msg_123456789",
          "senderId": 12345,
          "chatType": "private"
        },
        "status": "unread",
        "createdAt": "2025-11-12T22:30:00Z"
      }
    ],
    "unreadCount": 5,
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "hasMore": true
    }
  }
}
```

#### 1.2 标记通知已读
**接口地址**: `PUT /api/v1/notifications/{notificationId}/read`

#### 1.3 批量标记已读
**接口地址**: `PUT /api/v1/notifications/batch-read`

**请求参数**:
```json
{
  "notificationIds": ["notif_123", "notif_456"]
}
```

---

## 🌐 WebSocket 实时通信接口

### 连接地址
- **WebSocket URL**: `ws://localhost:9000/ws`
- **认证方式**: 连接时携带token参数

### 消息格式
```json
{
  "type": "message_type",
  "data": {},
  "timestamp": "2025-11-12T22:30:00Z",
  "messageId": "ws_msg_123"
}
```

### 消息类型

#### 1. 实时消息推送
```json
{
  "type": "new_message",
  "data": {
    "messageId": "msg_123456789",
    "senderId": 12345,
    "senderName": "张三",
    "chatType": "private",
    "chatId": 67890,
    "messageType": "text",
    "content": "Hello World",
    "timestamp": "2025-11-12T22:30:00Z"
  }
}
```

#### 2. 在线状态更新
```json
{
  "type": "user_status",
  "data": {
    "userId": 12345,
    "status": "online",
    "lastActiveTime": "2025-11-12T22:30:00Z"
  }
}
```

#### 3. 输入状态提示
```json
{
  "type": "typing_status",
  "data": {
    "userId": 12345,
    "chatType": "private",
    "chatId": 67890,
    "isTyping": true
  }
}
```

---

## ❌ 错误码定义

### HTTP状态码
| 状态码 | 说明 | 使用场景 |
|-------|------|---------|
| 200 | 成功 | 请求成功处理 |
| 201 | 创建成功 | 资源创建成功 |
| 400 | 请求错误 | 参数错误、格式错误 |
| 401 | 未授权 | 未登录或token无效 |
| 403 | 禁止访问 | 权限不足 |
| 404 | 资源不存在 | 请求的资源不存在 |
| 409 | 冲突 | 资源冲突，如用户名已存在 |
| 429 | 请求过频 | 触发限流 |
| 500 | 服务器错误 | 内部服务器错误 |

### 业务错误码
| 错误码 | 错误信息 | 说明 |
|-------|---------|------|
| 10001 | 用户名或密码错误 | 登录失败 |
| 10002 | 用户不存在 | 用户查询失败 |
| 10003 | 用户名已存在 | 注册失败 |
| 10004 | 邮箱已被注册 | 注册失败 |
| 10005 | 验证码错误 | 验证失败 |
| 20001 | 消息发送失败 | 消息服务异常 |
| 20002 | 消息不存在 | 消息查询失败 |
| 20003 | 消息已撤回 | 消息状态异常 |
| 30001 | 群组不存在 | 群组查询失败 |
| 30002 | 不是群组成员 | 权限验证失败 |
| 30003 | 群组已满 | 加入群组失败 |
| 40001 | 文件上传失败 | 文件服务异常 |
| 40002 | 文件格式不支持 | 文件类型错误 |
| 40003 | 文件大小超限 | 文件过大 |

### 错误响应格式
```json
{
  "code": 10001,
  "message": "用户名或密码错误",
  "data": null,
  "timestamp": "2025-11-12T22:30:00Z",
  "traceId": "abc123def456",
  "path": "/api/v1/users/login"
}
```

---

## 🔐 接口安全规范

### 1. 认证机制
- **JWT Token**: 使用JWT进行用户认证
- **Token刷新**: 支持refresh token机制
- **Token过期**: access token 2小时，refresh token 7天

### 2. 权限控制
- **RBAC**: 基于角色的访问控制
- **资源权限**: 细粒度的资源访问控制
- **接口权限**: 不同角色访问不同接口

### 3. 数据安全
- **HTTPS**: 所有接口强制使用HTTPS
- **参数验证**: 严格的参数校验和过滤
- **SQL注入防护**: 使用参数化查询
- **XSS防护**: 输出内容转义

### 4. 限流策略
- **用户级限流**: 每用户每分钟100次请求
- **IP级限流**: 每IP每分钟1000次请求
- **接口级限流**: 敏感接口特殊限制

---

## 📊 接口监控指标

### 性能指标
- **响应时间**: 95%请求 < 200ms
- **吞吐量**: 支持10000 QPS
- **可用性**: 99.9%可用性
- **错误率**: < 0.1%

### 监控维度
- **接口维度**: 每个接口的调用量、响应时间、错误率
- **用户维度**: 用户行为分析、活跃度统计
- **系统维度**: 系统资源使用情况、性能瓶颈

---

**文档版本**: v1.0.0  
**创建日期**: 2025-11-12  
**维护人**: 后端开发团队
