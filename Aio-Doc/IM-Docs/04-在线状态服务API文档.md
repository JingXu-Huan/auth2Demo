# 在线状态服务 API 文档

## 服务信息
- **服务名称**: presence-service
- **端口**: 8004
- **基础路径**: /api/v1/presence
- **版本**: v1.0.0

---

## 核心接口

### 1. 用户上线
- **URL**: `/online`
- **Method**: `POST`
- **功能**: 标记用户上线状态

**Request**:
```json
{
  "userId": 10001,
  "deviceId": "device_123",
  "platform": "WEB"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "上线成功",
  "data": {
    "userId": 10001,
    "status": "ONLINE",
    "lastActiveTime": "2025-11-09T15:00:00Z"
  }
}
```

---

### 2. 用户下线
- **URL**: `/offline`
- **Method**: `POST`
- **功能**: 标记用户下线

**Request**:
```json
{
  "userId": 10001,
  "deviceId": "device_123"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "下线成功",
  "data": null
}
```

---

### 3. 查询用户状态
- **URL**: `/status/{userId}`
- **Method**: `GET`
- **功能**: 查询用户在线状态

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 10001,
    "status": "ONLINE",
    "lastActiveTime": "2025-11-09T15:00:00Z",
    "devices": [
      {
        "deviceId": "device_123",
        "platform": "WEB",
        "onlineTime": "2025-11-09T14:00:00Z"
      }
    ]
  }
}
```

---

### 4. 批量查询状态
- **URL**: `/status/batch`
- **Method**: `POST`
- **功能**: 批量查询多个用户的在线状态

**Request**:
```json
{
  "userIds": [10001, 10002, 10003]
}
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "userId": 10001,
      "status": "ONLINE"
    },
    {
      "userId": 10002,
      "status": "OFFLINE"
    },
    {
      "userId": 10003,
      "status": "AWAY"
    }
  ]
}
```

---

### 5. 发送心跳
- **URL**: `/heartbeat`
- **Method**: `POST`
- **功能**: 保持在线状态

**Request**:
```json
{
  "userId": 10001,
  "deviceId": "device_123"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "心跳成功",
  "data": {
    "nextHeartbeatInterval": 30000
  }
}
```

---

## 在线状态类型

| 状态 | 说明 |
|------|------|
| ONLINE | 在线 |
| OFFLINE | 离线 |
| AWAY | 离开 |
| BUSY | 忙碌 |

---

## 数据模型

```typescript
interface PresenceStatus {
  userId: number;
  status: 'ONLINE' | 'OFFLINE' | 'AWAY' | 'BUSY';
  lastActiveTime: string;
  devices: Device[];
}

interface Device {
  deviceId: string;
  platform: 'WEB' | 'IOS' | 'ANDROID' | 'PC';
  onlineTime: string;
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-09  
**维护人**: 开发团队
