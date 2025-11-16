# 音视频服务 API 文档

## 📋 服务功能说明

音视频服务（RTC）为IM系统提供实时音视频通话能力，支持一对一语音/视频通话、多人音视频会议等功能。本服务基于WebRTC技术，提供低延迟、高质量的实时通信体验，并支持屏幕共享、录制、美颜等高级功能。

### 核心功能

#### 1. 语音通话
- **一对一通话**: 高清语音通话，低延迟
- **多人语音**: 支持最多50人同时语音
- **通话质量**: 自适应码率，保证通话质量
- **降噪处理**: AI降噪，消除背景噪音
- **回音消除**: 自动回音消除

#### 2. 视频通话
- **一对一视频**: 720P/1080P高清视频
- **多人视频**: 支持最多16人视频会议
- **画面布局**: 宫格、演讲者、画中画等多种布局
- **美颜滤镜**: 实时美颜、滤镜效果
- **虚拟背景**: 背景虚化、自定义背景

#### 3. 屏幕共享
- **全屏共享**: 共享整个屏幕
- **窗口共享**: 共享指定应用窗口
- **标注功能**: 屏幕标注、画笔工具
- **远程控制**: 授权远程控制（可选）

#### 4. 会议管理
- **创建会议**: 快速创建音视频会议
- **会议邀请**: 邀请链接、会议号
- **会议控制**: 主持人控制（静音、踢人、锁定）
- **举手发言**: 参会者举手申请发言
- **会议录制**: 云端录制会议内容

#### 5. 通话功能
- **呼叫/接听**: 发起呼叫、接听、拒绝、挂断
- **通话保持**: 通话保持、恢复
- **通话转接**: 转接给其他用户
- **多方通话**: 通话中添加其他成员
- **通话记录**: 记录所有通话历史

#### 6. 网络优化
- **自适应码率**: 根据网络自动调整码率
- **弱网优化**: 弱网环境下保证通话质量
- **断线重连**: 网络中断自动重连
- **丢包补偿**: FEC前向纠错、ARQ重传

### 技术特性
- **通信协议**: WebRTC
- **信令服务**: WebSocket + SDP/ICE
- **媒体服务器**: Janus / Kurento
- **TURN服务**: Coturn（NAT穿透）
- **录制存储**: OSS（云端录制）
- **服务发现**: Nacos

---

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
