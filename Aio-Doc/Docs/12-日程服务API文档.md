# 日程服务 API 文档

## 服务信息
- **服务名称**: calendar-service
- **端口**: 8012
- **基础路径**: /api/v1/calendar
- **版本**: v1.0.0

---

## 📋 服务功能说明

### 核心功能
日程服务是企业协作的重要组成部分，提供完整的日程管理、会议安排、提醒通知等功能，帮助团队高效协作。

### 主要特性

#### 1. 日程管理
- ✅ **创建日程**: 支持单次、重复日程
- ✅ **日程编辑**: 修改日程信息、时间、参与人
- ✅ **日程删除**: 删除单次或系列日程
- ✅ **日程查询**: 按时间范围、类型、状态查询
- ✅ **日程分类**: 会议、任务、提醒、纪念日等
- ✅ **日程共享**: 与他人共享日程信息

#### 2. 重复日程
- ✅ **重复规则**: 每天、每周、每月、每年
- ✅ **自定义重复**: 工作日、周末、指定日期
- ✅ **结束条件**: 永不结束、指定次数、指定日期
- ✅ **例外处理**: 跳过特定日期

#### 3. 参与人管理
- ✅ **邀请参与**: 邀请用户参加日程
- ✅ **接受/拒绝**: 参与人响应邀请
- ✅ **权限控制**: 查看、编辑、管理权限
- ✅ **状态同步**: 实时同步参与状态

#### 4. 提醒功能
- ✅ **多种提醒**: 邮件、短信、应用内通知
- ✅ **提前提醒**: 5分钟、15分钟、1小时、1天等
- ✅ **重复提醒**: 支持多次提醒
- ✅ **智能提醒**: 根据路程时间提醒

#### 5. 日历视图
- ✅ **日视图**: 查看单日日程
- ✅ **周视图**: 查看一周日程
- ✅ **月视图**: 查看月度日程
- ✅ **议程视图**: 列表形式查看
- ✅ **多日历**: 支持多个日历切换

#### 6. 日程冲突检测
- ✅ **时间冲突**: 自动检测时间冲突
- ✅ **建议时间**: 推荐可用时间段
- ✅ **忙碌状态**: 显示用户忙碌/空闲

#### 7. 日历订阅
- ✅ **订阅他人日历**: 查看他人公开日历
- ✅ **团队日历**: 部门、项目组日历
- ✅ **节假日日历**: 自动同步节假日
- ✅ **iCal导入导出**: 支持标准日历格式

### 业务场景

#### 场景1: 会议安排
```
用户A创建会议日程
  ↓
邀请参与人B、C、D
  ↓
系统检测时间冲突
  ↓
发送邀请通知
  ↓
参与人接受/拒绝
  ↓
会议前15分钟提醒
  ↓
会议开始，自动创建会议室
```

#### 场景2: 重复任务
```
用户创建每周例会
  ↓
设置重复规则：每周一10:00
  ↓
设置结束时间：3个月后
  ↓
系统自动生成12次日程
  ↓
每次会议前自动提醒
```

#### 场景3: 团队协作
```
项目经理创建项目日历
  ↓
添加项目里程碑
  ↓
团队成员订阅日历
  ↓
自动同步到个人日历
  ↓
关键节点提前提醒
```

### 技术特点

#### 1. 高性能
- 使用 Redis 缓存热点日程数据
- 分页查询优化
- 索引优化（时间范围查询）

#### 2. 高可用
- 提醒服务独立部署
- 消息队列异步处理
- 失败重试机制

#### 3. 数据一致性
- 事务保证数据一致
- 乐观锁处理并发
- 分布式锁防止重复

#### 4. 扩展性
- 支持自定义字段
- 插件化提醒方式
- 开放API接口

---

## 目录
1. [创建日程](#1-创建日程)
2. [获取日程详情](#2-获取日程详情)
3. [更新日程](#3-更新日程)
4. [删除日程](#4-删除日程)
5. [查询日程列表](#5-查询日程列表)
6. [邀请参与人](#6-邀请参与人)
7. [响应邀请](#7-响应邀请)
8. [设置提醒](#8-设置提醒)
9. [检测时间冲突](#9-检测时间冲突)
10. [订阅日历](#10-订阅日历)

---

## 1. 创建日程

### 接口信息
- **URL**: `/events`
- **Method**: `POST`
- **功能**: 创建新日程
- **认证**: 需要 Bearer Token

### Request
```json
{
  "title": "项目评审会议",
  "description": "讨论Q1项目进展",
  "startTime": "2025-11-15T10:00:00Z",
  "endTime": "2025-11-15T11:30:00Z",
  "location": "会议室A",
  "eventType": "MEETING",
  "isAllDay": false,
  "attendees": [10002, 10003, 10004],
  "reminders": [
    {
      "method": "NOTIFICATION",
      "minutes": 15
    },
    {
      "method": "EMAIL",
      "minutes": 60
    }
  ],
  "recurrence": {
    "frequency": "WEEKLY",
    "interval": 1,
    "daysOfWeek": ["MONDAY"],
    "endDate": "2026-02-15T00:00:00Z"
  },
  "visibility": "PUBLIC",
  "color": "#FF5733"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 日程标题 |
| description | string | 否 | 日程描述 |
| startTime | string | 是 | 开始时间（ISO 8601） |
| endTime | string | 是 | 结束时间 |
| location | string | 否 | 地点 |
| eventType | string | 是 | 类型：MEETING/TASK/REMINDER/ANNIVERSARY |
| isAllDay | boolean | 否 | 是否全天，默认false |
| attendees | array | 否 | 参与人ID列表 |
| reminders | array | 否 | 提醒设置 |
| recurrence | object | 否 | 重复规则 |
| visibility | string | 否 | 可见性：PUBLIC/PRIVATE，默认PUBLIC |
| color | string | 否 | 日程颜色 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "eventId": "evt_123456",
    "title": "项目评审会议",
    "description": "讨论Q1项目进展",
    "startTime": "2025-11-15T10:00:00Z",
    "endTime": "2025-11-15T11:30:00Z",
    "location": "会议室A",
    "eventType": "MEETING",
    "creatorId": 10001,
    "creatorName": "张三",
    "attendees": [
      {
        "userId": 10002,
        "name": "李四",
        "status": "PENDING"
      }
    ],
    "reminders": [
      {
        "method": "NOTIFICATION",
        "minutes": 15
      }
    ],
    "recurrence": {
      "frequency": "WEEKLY",
      "interval": 1,
      "daysOfWeek": ["MONDAY"],
      "count": 12
    },
    "hasConflict": false,
    "createdAt": "2025-11-10T12:00:00Z"
  }
}
```

---

## 2. 获取日程详情

### 接口信息
- **URL**: `/events/{eventId}`
- **Method**: `GET`
- **功能**: 获取日程详细信息
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/calendar/events/evt_123456
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "eventId": "evt_123456",
    "title": "项目评审会议",
    "description": "讨论Q1项目进展",
    "startTime": "2025-11-15T10:00:00Z",
    "endTime": "2025-11-15T11:30:00Z",
    "location": "会议室A",
    "eventType": "MEETING",
    "isAllDay": false,
    "creatorId": 10001,
    "creatorName": "张三",
    "creatorAvatar": "https://cdn.example.com/avatar/10001.jpg",
    "attendees": [
      {
        "userId": 10002,
        "name": "李四",
        "avatar": "https://cdn.example.com/avatar/10002.jpg",
        "status": "ACCEPTED",
        "respondedAt": "2025-11-10T13:00:00Z"
      }
    ],
    "reminders": [
      {
        "reminderId": "rem_001",
        "method": "NOTIFICATION",
        "minutes": 15
      }
    ],
    "recurrence": {
      "frequency": "WEEKLY",
      "interval": 1,
      "daysOfWeek": ["MONDAY"],
      "endDate": "2026-02-15T00:00:00Z"
    },
    "visibility": "PUBLIC",
    "color": "#FF5733",
    "attachments": [],
    "createdAt": "2025-11-10T12:00:00Z",
    "updatedAt": "2025-11-10T12:00:00Z"
  }
}
```

---

## 3. 更新日程

### 接口信息
- **URL**: `/events/{eventId}`
- **Method**: `PUT`
- **功能**: 更新日程信息
- **认证**: 需要 Bearer Token
- **权限**: 创建者或有编辑权限的参与人

### Request
```json
{
  "title": "项目评审会议（更新）",
  "startTime": "2025-11-15T14:00:00Z",
  "endTime": "2025-11-15T15:30:00Z",
  "location": "会议室B",
  "updateType": "THIS_EVENT"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| updateType | string | 否 | THIS_EVENT/ALL_EVENTS，默认THIS_EVENT |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "eventId": "evt_123456",
    "title": "项目评审会议（更新）",
    "startTime": "2025-11-15T14:00:00Z",
    "endTime": "2025-11-15T15:30:00Z",
    "updatedAt": "2025-11-10T14:00:00Z"
  }
}
```

---

## 4. 删除日程

### 接口信息
- **URL**: `/events/{eventId}`
- **Method**: `DELETE`
- **功能**: 删除日程
- **认证**: 需要 Bearer Token
- **权限**: 仅创建者

### Request
```
DELETE /api/v1/calendar/events/evt_123456?deleteType=THIS_EVENT
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deleteType | string | 否 | THIS_EVENT/ALL_EVENTS，默认THIS_EVENT |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

## 5. 查询日程列表

### 接口信息
- **URL**: `/events`
- **Method**: `GET`
- **功能**: 查询日程列表
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/calendar/events?startDate=2025-11-01&endDate=2025-11-30&eventType=MEETING&status=ACCEPTED
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | string | 是 | 开始日期 |
| endDate | string | 是 | 结束日期 |
| eventType | string | 否 | 日程类型筛选 |
| status | string | 否 | 参与状态筛选 |
| calendarId | string | 否 | 日历ID筛选 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "events": [
      {
        "eventId": "evt_123456",
        "title": "项目评审会议",
        "startTime": "2025-11-15T10:00:00Z",
        "endTime": "2025-11-15T11:30:00Z",
        "eventType": "MEETING",
        "status": "ACCEPTED",
        "attendeeCount": 4,
        "color": "#FF5733"
      }
    ],
    "total": 15
  }
}
```

---

## 6. 邀请参与人

### 接口信息
- **URL**: `/events/{eventId}/attendees`
- **Method**: `POST`
- **功能**: 邀请参与人
- **认证**: 需要 Bearer Token

### Request
```json
{
  "userIds": [10005, 10006],
  "message": "请参加项目评审会议"
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "邀请已发送",
  "data": {
    "eventId": "evt_123456",
    "invitedCount": 2,
    "attendees": [
      {
        "userId": 10005,
        "name": "王五",
        "status": "PENDING"
      }
    ]
  }
}
```

---

## 7. 响应邀请

### 接口信息
- **URL**: `/events/{eventId}/respond`
- **Method**: `POST`
- **功能**: 接受或拒绝日程邀请
- **认证**: 需要 Bearer Token

### Request
```json
{
  "status": "ACCEPTED",
  "comment": "准时参加"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 是 | ACCEPTED/DECLINED/TENTATIVE |
| comment | string | 否 | 回复备注 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "已接受邀请",
  "data": {
    "eventId": "evt_123456",
    "userId": 10002,
    "status": "ACCEPTED",
    "respondedAt": "2025-11-10T15:00:00Z"
  }
}
```

---

## 8. 设置提醒

### 接口信息
- **URL**: `/events/{eventId}/reminders`
- **Method**: `POST`
- **功能**: 设置日程提醒
- **认证**: 需要 Bearer Token

### Request
```json
{
  "reminders": [
    {
      "method": "NOTIFICATION",
      "minutes": 15
    },
    {
      "method": "EMAIL",
      "minutes": 1440
    }
  ]
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "提醒设置成功",
  "data": {
    "eventId": "evt_123456",
    "reminders": [
      {
        "reminderId": "rem_001",
        "method": "NOTIFICATION",
        "minutes": 15,
        "triggerTime": "2025-11-15T09:45:00Z"
      }
    ]
  }
}
```

---

## 9. 检测时间冲突

### 接口信息
- **URL**: `/events/check-conflict`
- **Method**: `POST`
- **功能**: 检测时间冲突
- **认证**: 需要 Bearer Token

### Request
```json
{
  "startTime": "2025-11-15T10:00:00Z",
  "endTime": "2025-11-15T11:30:00Z",
  "attendees": [10002, 10003]
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "hasConflict": true,
    "conflicts": [
      {
        "userId": 10002,
        "userName": "李四",
        "conflictEvents": [
          {
            "eventId": "evt_789",
            "title": "技术分享会",
            "startTime": "2025-11-15T10:30:00Z",
            "endTime": "2025-11-15T12:00:00Z"
          }
        ]
      }
    ],
    "suggestedTimes": [
      {
        "startTime": "2025-11-15T14:00:00Z",
        "endTime": "2025-11-15T15:30:00Z"
      }
    ]
  }
}
```

---

## 10. 订阅日历

### 接口信息
- **URL**: `/calendars/{calendarId}/subscribe`
- **Method**: `POST`
- **功能**: 订阅日历
- **认证**: 需要 Bearer Token

### Request
```json
{
  "color": "#4CAF50",
  "notifications": true
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "订阅成功",
  "data": {
    "calendarId": "cal_001",
    "name": "技术部日历",
    "ownerId": 10001,
    "subscribedAt": "2025-11-10T16:00:00Z"
  }
}
```

---

## 数据模型

### Event
```typescript
interface Event {
  eventId: string;              // 日程ID
  title: string;                // 标题
  description: string;          // 描述
  startTime: string;            // 开始时间
  endTime: string;              // 结束时间
  location: string;             // 地点
  eventType: EventType;         // 类型
  isAllDay: boolean;            // 是否全天
  creatorId: number;            // 创建者ID
  attendees: Attendee[];        // 参与人列表
  reminders: Reminder[];        // 提醒列表
  recurrence: Recurrence;       // 重复规则
  visibility: Visibility;       // 可见性
  color: string;                // 颜色
  createdAt: string;            // 创建时间
  updatedAt: string;            // 更新时间
}

enum EventType {
  MEETING = 'MEETING',          // 会议
  TASK = 'TASK',                // 任务
  REMINDER = 'REMINDER',        // 提醒
  ANNIVERSARY = 'ANNIVERSARY'   // 纪念日
}

enum AttendeeStatus {
  PENDING = 'PENDING',          // 待响应
  ACCEPTED = 'ACCEPTED',        // 已接受
  DECLINED = 'DECLINED',        // 已拒绝
  TENTATIVE = 'TENTATIVE'       // 待定
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-11  
**维护人**: 开发团队
