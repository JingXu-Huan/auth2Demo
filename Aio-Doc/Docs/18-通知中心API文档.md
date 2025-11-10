# 通知中心 API 文档

## 服务信息
- **服务名称**: notification-service
- **端口**: 8018
- **基础路径**: /api/v1/notifications
- **版本**: v1.0.0

---

## 📋 服务功能说明

### 核心功能
通知中心提供统一的消息通知管理，支持应用内通知、邮件通知、短信通知等多种通知方式，确保重要信息及时送达。

### 主要特性

#### 1. 通知类型
- ✅ **系统通知**: 系统级消息
- ✅ **业务通知**: 业务相关消息
- ✅ **互动通知**: @提醒、评论、点赞
- ✅ **审批通知**: 审批相关通知
- ✅ **日程通知**: 日程提醒
- ✅ **任务通知**: 任务提醒

#### 2. 通知渠道
- ✅ **应用内通知**: 应用内消息
- ✅ **邮件通知**: 发送邮件
- ✅ **短信通知**: 发送短信
- ✅ **推送通知**: App推送
- ✅ **企业微信**: 企业微信通知
- ✅ **钉钉**: 钉钉通知

#### 3. 通知管理
- ✅ **查看通知**: 查看通知列表
- ✅ **标记已读**: 标记为已读
- ✅ **删除通知**: 删除通知
- ✅ **通知设置**: 个性化设置
- ✅ **免打扰**: 勿扰模式

#### 4. 订阅管理
- ✅ **订阅主题**: 订阅感兴趣的主题
- ✅ **取消订阅**: 取消订阅
- ✅ **订阅设置**: 设置订阅偏好

---

## 1. 发送通知

### Request
```json
{
  "type": "SYSTEM",
  "title": "系统升级通知",
  "content": "系统将于今晚22:00进行升级维护",
  "priority": "HIGH",
  "receivers": [10001, 10002, 10003],
  "channels": ["APP", "EMAIL"],
  "data": {
    "url": "https://example.com/notice/123"
  }
}
```

### Response
```json
{
  "code": 200,
  "message": "发送成功",
  "data": {
    "notificationId": "ntf_123456",
    "sentCount": 3,
    "failedCount": 0,
    "sentAt": "2025-11-10T12:00:00Z"
  }
}
```

---

## 2. 获取通知列表

### Request
```
GET /api/v1/notifications?type=SYSTEM&status=UNREAD&page=1&size=20
```

### Response
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "unreadCount": 15,
    "notifications": [
      {
        "notificationId": "ntf_123456",
        "type": "SYSTEM",
        "title": "系统升级通知",
        "content": "系统将于今晚22:00进行升级维护",
        "priority": "HIGH",
        "status": "UNREAD",
        "data": {
          "url": "https://example.com/notice/123"
        },
        "createdAt": "2025-11-10T12:00:00Z"
      }
    ]
  }
}
```

---

## 3. 标记已读

### Request
```json
{
  "notificationIds": ["ntf_123456", "ntf_123457"]
}
```

### Response
```json
{
  "code": 200,
  "message": "已标记为已读",
  "data": {
    "readCount": 2
  }
}
```

---

## 4. 通知设置

### Request
```json
{
  "settings": {
    "SYSTEM": {
      "enabled": true,
      "channels": ["APP", "EMAIL"]
    },
    "TASK": {
      "enabled": true,
      "channels": ["APP"]
    },
    "APPROVAL": {
      "enabled": true,
      "channels": ["APP", "EMAIL", "SMS"]
    }
  },
  "doNotDisturb": {
    "enabled": true,
    "startTime": "22:00",
    "endTime": "08:00"
  }
}
```

### Response
```json
{
  "code": 200,
  "message": "设置成功",
  "data": {
    "userId": 10001,
    "updatedAt": "2025-11-10T12:00:00Z"
  }
}
```

---

## 数据模型

### Notification
```typescript
interface Notification {
  notificationId: string;       // 通知ID
  type: NotificationType;       // 通知类型
  title: string;                // 标题
  content: string;              // 内容
  priority: Priority;           // 优先级
  status: NotificationStatus;   // 状态
  senderId: number;             // 发送者ID
  receiverId: number;           // 接收者ID
  channels: Channel[];          // 发送渠道
  data: object;                 // 附加数据
  createdAt: string;            // 创建时间
  readAt: string;               // 阅读时间
}

enum NotificationType {
  SYSTEM = 'SYSTEM',            // 系统通知
  BUSINESS = 'BUSINESS',        // 业务通知
  INTERACTION = 'INTERACTION',  // 互动通知
  APPROVAL = 'APPROVAL',        // 审批通知
  CALENDAR = 'CALENDAR',        // 日程通知
  TASK = 'TASK'                 // 任务通知
}

enum NotificationStatus {
  UNREAD = 'UNREAD',            // 未读
  READ = 'READ',                // 已读
  DELETED = 'DELETED'           // 已删除
}

enum Channel {
  APP = 'APP',                  // 应用内
  EMAIL = 'EMAIL',              // 邮件
  SMS = 'SMS',                  // 短信
  PUSH = 'PUSH',                // 推送
  WECHAT = 'WECHAT',            // 企业微信
  DINGTALK = 'DINGTALK'         // 钉钉
}

enum Priority {
  LOW = 'LOW',                  // 低
  MEDIUM = 'MEDIUM',            // 中
  HIGH = 'HIGH',                // 高
  URGENT = 'URGENT'             // 紧急
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-11  
**维护人**: 开发团队
