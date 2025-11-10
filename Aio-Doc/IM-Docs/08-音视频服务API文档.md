# 音视频服务 API 文档

## 服务信息
- **服务名称**: rtc-service
- **端口**: 8008
- **基础路径**: /api/v1/rtc
- **版本**: v1.0.0

---

## 核心接口

### 1. 发起通话
- **URL**: `/call/initiate`
- **Method**: `POST`
- **功能**: 发起音视频通话

**Request**:
```json
{
  "callerId": 10001,
  "calleeId": 10002,
  "callType": "VIDEO"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "通话发起成功",
  "data": {
    "callId": "call_123456",
    "callerId": 10001,
    "calleeId": 10002,
    "callType": "VIDEO",
    "status": "CALLING",
    "createdAt": "2025-11-09T15:00:00Z"
  }
}
```

---

### 2. 接听通话
- **URL**: `/call/answer`
- **Method**: `POST`
- **功能**: 接听通话

**Request**:
```json
{
  "callId": "call_123456",
  "userId": 10002
}
```

**Response**:
```json
{
  "code": 200,
  "message": "接听成功",
  "data": {
    "callId": "call_123456",
    "status": "CONNECTED",
    "connectedAt": "2025-11-09T15:00:30Z"
  }
}
```

---

### 3. 拒绝通话
- **URL**: `/call/reject`
- **Method**: `POST`
- **功能**: 拒绝通话

**Request**:
```json
{
  "callId": "call_123456",
  "userId": 10002,
  "reason": "BUSY"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "已拒绝",
  "data": null
}
```

---

### 4. 挂断通话
- **URL**: `/call/hangup`
- **Method**: `POST`
- **功能**: 挂断通话

**Request**:
```json
{
  "callId": "call_123456",
  "userId": 10001
}
```

**Response**:
```json
{
  "code": 200,
  "message": "通话已结束",
  "data": {
    "callId": "call_123456",
    "duration": 120,
    "endedAt": "2025-11-09T15:02:30Z"
  }
}
```

---

### 5. 获取通话记录
- **URL**: `/call/history`
- **Method**: `GET`
- **功能**: 获取通话历史记录

**Request**:
```
GET /api/v1/rtc/call/history?userId=10001&page=1&size=20
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 30,
    "page": 1,
    "size": 20,
    "records": [
      {
        "callId": "call_123456",
        "callerId": 10001,
        "callerName": "张三",
        "calleeId": 10002,
        "calleeName": "李四",
        "callType": "VIDEO",
        "status": "COMPLETED",
        "duration": 120,
        "startTime": "2025-11-09T15:00:00Z",
        "endTime": "2025-11-09T15:02:00Z"
      }
    ]
  }
}
```

---

### 6. 获取WebRTC配置
- **URL**: `/config/webrtc`
- **Method**: `GET`
- **功能**: 获取WebRTC连接配置

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "iceServers": [
      {
        "urls": "stun:stun.example.com:3478"
      },
      {
        "urls": "turn:turn.example.com:3478",
        "username": "user",
        "credential": "pass"
      }
    ],
    "sdpSemantics": "unified-plan"
  }
}
```

---

## 通话类型

| 类型 | 说明 |
|------|------|
| AUDIO | 语音通话 |
| VIDEO | 视频通话 |

---

## 通话状态

| 状态 | 说明 |
|------|------|
| CALLING | 呼叫中 |
| RINGING | 响铃中 |
| CONNECTED | 已接通 |
| COMPLETED | 已完成 |
| REJECTED | 已拒绝 |
| MISSED | 未接听 |
| CANCELLED | 已取消 |

---

## 拒绝原因

| 原因 | 说明 |
|------|------|
| BUSY | 忙碌 |
| DECLINED | 主动拒绝 |
| NO_ANSWER | 无应答 |

---

## WebRTC集成示例

```javascript
// 1. 获取WebRTC配置
const config = await fetch('/api/v1/rtc/config/webrtc').then(r => r.json())

// 2. 创建PeerConnection
const pc = new RTCPeerConnection(config.data)

// 3. 获取本地媒体流
const localStream = await navigator.mediaDevices.getUserMedia({
  video: true,
  audio: true
})

// 4. 添加到PeerConnection
localStream.getTracks().forEach(track => {
  pc.addTrack(track, localStream)
})

// 5. 创建Offer
const offer = await pc.createOffer()
await pc.setLocalDescription(offer)

// 6. 发送Offer到对方
// 通过信令服务器发送
```

---

## 数据模型

```typescript
interface CallRecord {
  callId: string;
  callerId: number;
  callerName: string;
  calleeId: number;
  calleeName: string;
  callType: 'AUDIO' | 'VIDEO';
  status: CallStatus;
  duration: number;
  startTime: string;
  endTime: string;
}

type CallStatus = 'CALLING' | 'RINGING' | 'CONNECTED' | 'COMPLETED' | 'REJECTED' | 'MISSED' | 'CANCELLED';
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-09  
**维护人**: 开发团队
