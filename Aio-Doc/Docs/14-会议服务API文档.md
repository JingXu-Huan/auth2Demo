# 会议服务 API 文档

## 服务信息
- **服务名称**: meeting-service
- **端口**: 8014
- **基础路径**: /api/v1/meetings
- **版本**: v1.0.0

---

## 📋 服务功能说明

### 核心功能
会议服务提供完整的会议管理、预约、录制、回放等功能，支持音视频会议、屏幕共享、会议纪要等企业级会议需求。

### 主要特性

#### 1. 会议管理
- ✅ **创建会议**: 即时会议、预约会议
- ✅ **会议邀请**: 邀请参会人员
- ✅ **会议室预订**: 预订线下会议室
- ✅ **会议取消**: 取消会议并通知
- ✅ **会议重复**: 周期性会议

#### 2. 会议功能
- ✅ **音视频通话**: 高清音视频
- ✅ **屏幕共享**: 共享屏幕内容
- ✅ **白板协作**: 在线白板
- ✅ **文档演示**: PPT/PDF演示
- ✅ **会议录制**: 云端录制
- ✅ **实时字幕**: AI语音转文字

#### 3. 参会管理
- ✅ **参会确认**: 接受/拒绝邀请
- ✅ **参会状态**: 在线/离线状态
- ✅ **发言控制**: 主持人控制
- ✅ **举手发言**: 申请发言
- ✅ **禁言管理**: 静音控制

#### 4. 会议纪要
- ✅ **自动纪要**: AI生成会议纪要
- ✅ **手动记录**: 人工编辑纪要
- ✅ **待办事项**: 提取待办任务
- ✅ **纪要分享**: 分享给参会人

#### 5. 会议统计
- ✅ **参会统计**: 参会人数、时长
- ✅ **会议质量**: 网络质量统计
- ✅ **使用分析**: 会议使用情况

### 业务场景

#### 场景1: 快速发起会议
```
用户点击"发起会议"
  ↓
选择参会人员
  ↓
系统生成会议链接
  ↓
发送邀请通知
  ↓
参会人员加入会议
```

#### 场景2: 预约会议
```
用户创建预约会议
  ↓
设置会议时间、主题
  ↓
邀请参会人员
  ↓
系统检测时间冲突
  ↓
会议前15分钟提醒
  ↓
自动开启会议室
```

---

## 目录
1. [创建会议](#1-创建会议)
2. [加入会议](#2-加入会议)
3. [结束会议](#3-结束会议)
4. [会议录制](#4-会议录制)
5. [会议纪要](#5-会议纪要)

---

## 1. 创建会议

### 接口信息
- **URL**: `/`
- **Method**: `POST`
- **功能**: 创建新会议
- **认证**: 需要 Bearer Token

### Request
```json
{
  "title": "产品评审会议",
  "description": "讨论Q1产品规划",
  "startTime": "2025-11-15T14:00:00Z",
  "duration": 60,
  "meetingType": "VIDEO",
  "participants": [10002, 10003, 10004],
  "settings": {
    "enableRecording": true,
    "enableWaitingRoom": false,
    "muteOnEntry": true,
    "allowScreenShare": true
  }
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "meetingId": "mtg_123456",
    "title": "产品评审会议",
    "meetingNumber": "123 456 789",
    "joinUrl": "https://meeting.example.com/j/123456789",
    "password": "abc123",
    "hostId": 10001,
    "startTime": "2025-11-15T14:00:00Z",
    "duration": 60,
    "createdAt": "2025-11-10T12:00:00Z"
  }
}
```

---

## 2. 加入会议

### 接口信息
- **URL**: `/{meetingId}/join`
- **Method**: `POST`

### Request
```json
{
  "password": "abc123",
  "displayName": "张三",
  "audioEnabled": true,
  "videoEnabled": true
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "加入成功",
  "data": {
    "meetingId": "mtg_123456",
    "participantId": "ppt_001",
    "webrtcUrl": "wss://webrtc.example.com/mtg_123456",
    "token": "webrtc_token_xxx"
  }
}
```

---

## 数据模型

### Meeting
```typescript
interface Meeting {
  meetingId: string;            // 会议ID
  title: string;                // 会议主题
  description: string;          // 会议描述
  meetingNumber: string;        // 会议号
  meetingType: MeetingType;     // 会议类型
  hostId: number;               // 主持人ID
  startTime: string;            // 开始时间
  duration: number;             // 时长(分钟)
  status: MeetingStatus;        // 会议状态
  joinUrl: string;              // 加入链接
  password: string;             // 会议密码
  participants: Participant[];  // 参会人员
  recordingUrl: string;         // 录制URL
  createdAt: string;            // 创建时间
}

enum MeetingType {
  AUDIO = 'AUDIO',              // 语音会议
  VIDEO = 'VIDEO',              // 视频会议
  WEBINAR = 'WEBINAR'           // 网络研讨会
}

enum MeetingStatus {
  SCHEDULED = 'SCHEDULED',      // 已预约
  STARTED = 'STARTED',          // 进行中
  ENDED = 'ENDED',              // 已结束
  CANCELLED = 'CANCELLED'       // 已取消
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-11  
**维护人**: 开发团队
