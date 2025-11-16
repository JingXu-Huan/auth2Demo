# 在线状态服务 API 文档

## 📋 服务功能说明

在线状态服务负责管理和同步用户的实时在线状态，为IM系统提供准确的用户状态信息。本服务通过心跳机制、WebSocket连接监控等方式，实时追踪用户的在线、离线、忙碌等状态，并将状态变化及时推送给相关用户。

### 核心功能

#### 1. 状态管理
- **在线状态**: ONLINE（在线）、OFFLINE（离线）、BUSY（忙碌）、AWAY（离开）
- **自动切换**: 根据用户活动自动切换状态（如5分钟无操作→离开）
- **手动设置**: 用户主动设置当前状态
- **状态持久化**: 状态信息存储，支持跨设备同步

#### 2. 状态同步
- **实时推送**: 好友状态变化实时推送
- **批量查询**: 批量获取多个用户的在线状态
- **状态订阅**: 订阅关注用户的状态变化
- **离线通知**: 用户离线时通知相关好友

#### 3. 心跳机制
- **心跳检测**: 定时发送心跳包，维持在线状态
- **超时判定**: 心跳超时自动标记为离线
- **断线重连**: 支持断线后快速恢复状态
- **多端状态**: 同一用户多设备在线状态管理

#### 4. 扩展信息
- **自定义状态**: 支持自定义状态文本（如"会议中"、"外出"）
- **状态图标**: 状态对应的图标和颜色
- **最后在线时间**: 记录用户最后活跃时间
- **设备信息**: 显示用户当前使用的设备类型

### 技术特性
- **缓存**: Redis（状态存储、快速查询）
- **消息推送**: WebSocket（实时状态推送）
- **心跳间隔**: 30秒
- **超时时间**: 60秒
- **服务发现**: Nacos

---

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
