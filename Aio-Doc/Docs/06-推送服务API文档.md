# 推送服务 API 文档

## 📋 服务功能说明

推送服务负责向用户设备发送各类推送通知，包括消息提醒、系统通知、营销推送等。本服务整合了多个推送渠道（APNs、FCM、华为、小米等），提供统一的推送接口，支持精准推送、定时推送、批量推送等多种推送方式。

### 核心功能

#### 1. 多渠道推送
- **iOS推送**: Apple Push Notification Service (APNs)
- **Android推送**: Firebase Cloud Messaging (FCM)
- **国内厂商**: 华为、小米、OPPO、vivo、魅族推送
- **Web推送**: Web Push Notification
- **自动选择**: 根据设备类型自动选择推送渠道

#### 2. 推送类型
- **消息推送**: 新消息提醒、@提醒、群消息
- **系统通知**: 好友申请、群邀请、系统公告
- **业务提醒**: 日程提醒、任务提醒、会议通知
- **营销推送**: 活动通知、功能推荐
- **静默推送**: 后台数据同步、状态更新

#### 3. 推送策略
- **即时推送**: 实时发送，毫秒级送达
- **定时推送**: 指定时间发送
- **批量推送**: 批量用户推送，支持分批发送
- **智能推送**: 根据用户活跃时间智能推送
- **防打扰**: 遵守用户免打扰时间设置

#### 4. 推送管理
- **推送记录**: 记录所有推送历史
- **送达统计**: 推送成功率、送达率统计
- **点击统计**: 推送点击率、转化率分析
- **失败重试**: 推送失败自动重试机制
- **推送撤回**: 支持撤回未送达的推送

#### 5. 用户设置
- **推送开关**: 总开关、分类开关
- **免打扰**: 设置免打扰时间段
- **推送声音**: 自定义推送提示音
- **震动设置**: 推送震动开关
- **角标管理**: App角标数字管理

#### 6. 高级功能
- **富媒体推送**: 支持图片、视频、音频
- **交互式推送**: 快捷回复、操作按钮
- **分组推送**: 按用户分组推送
- **A/B测试**: 推送内容A/B测试
- **推送模板**: 预定义推送模板

### 技术特性
- **推送网关**: APNs、FCM、厂商推送SDK
- **消息队列**: RabbitMQ（推送任务队列）
- **数据库**: MongoDB（推送记录）
- **缓存**: Redis（设备Token、推送配置）
- **定时任务**: Quartz（定时推送）
- **服务发现**: Nacos

---

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
