# 推送服务 API 文档

## 服务信息
- **服务名称**: push-service
- **端口**: 8006
- **基础路径**: /api/v1/push
- **版本**: v1.0.0

---

## 核心接口

### 1. 发送推送
- **URL**: `/send`
- **Method**: `POST`
- **功能**: 发送推送通知

**Request**:
```json
{
  "userId": 10001,
  "title": "新消息",
  "content": "您有一条新消息",
  "type": "MESSAGE",
  "data": {
    "messageId": "msg_123456",
    "senderId": 10002
  },
  "platforms": ["IOS", "ANDROID"]
}
```

**Response**:
```json
{
  "code": 200,
  "message": "推送成功",
  "data": {
    "pushId": "push_123456",
    "userId": 10001,
    "sentCount": 2,
    "failedCount": 0,
    "sentTime": "2025-11-09T15:00:00Z"
  }
}
```

---

### 2. 批量推送
- **URL**: `/send/batch`
- **Method**: `POST`
- **功能**: 批量发送推送

**Request**:
```json
{
  "userIds": [10001, 10002, 10003],
  "title": "系统通知",
  "content": "系统将于今晚维护",
  "type": "SYSTEM"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "批量推送成功",
  "data": {
    "totalCount": 3,
    "successCount": 3,
    "failedCount": 0
  }
}
```

---

### 3. 查询推送历史
- **URL**: `/history`
- **Method**: `GET`
- **功能**: 查询用户的推送历史

**Request**:
```
GET /api/v1/push/history?userId=10001&page=1&size=20
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "page": 1,
    "size": 20,
    "records": [
      {
        "pushId": "push_123456",
        "title": "新消息",
        "content": "您有一条新消息",
        "type": "MESSAGE",
        "status": "SENT",
        "sentTime": "2025-11-09T15:00:00Z",
        "readTime": "2025-11-09T15:05:00Z"
      }
    ]
  }
}
```

---

### 4. 标记已读
- **URL**: `/read/{pushId}`
- **Method**: `POST`
- **功能**: 标记推送为已读

**Response**:
```json
{
  "code": 200,
  "message": "标记成功",
  "data": null
}
```

---

### 5. 注册设备Token
- **URL**: `/device/register`
- **Method**: `POST`
- **功能**: 注册设备推送Token

**Request**:
```json
{
  "userId": 10001,
  "deviceId": "device_123",
  "platform": "IOS",
  "pushToken": "apns_token_xxxxx"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "deviceId": "device_123",
    "registeredAt": "2025-11-09T15:00:00Z"
  }
}
```

---

## 推送类型

| 类型 | 说明 |
|------|------|
| MESSAGE | 消息通知 |
| SYSTEM | 系统通知 |
| FRIEND_REQUEST | 好友请求 |
| GROUP_INVITE | 群组邀请 |
| ANNOUNCEMENT | 公告通知 |

---

## 推送平台

| 平台 | 推送服务 |
|------|----------|
| IOS | APNs (Apple Push Notification service) |
| ANDROID | FCM (Firebase Cloud Messaging) |
| WEB | WebSocket / Server-Sent Events |

---

## 数据模型

```typescript
interface PushNotification {
  pushId: string;
  userId: number;
  title: string;
  content: string;
  type: PushType;
  data?: any;
  status: 'PENDING' | 'SENT' | 'FAILED' | 'READ';
  sentTime: string;
  readTime?: string;
}

type PushType = 'MESSAGE' | 'SYSTEM' | 'FRIEND_REQUEST' | 'GROUP_INVITE' | 'ANNOUNCEMENT';
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-09  
**维护人**: 开发团队
