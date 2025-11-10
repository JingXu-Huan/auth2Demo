# 群组服务 API 文档

## 服务信息
- **服务名称**: group-service
- **端口**: 8003
- **基础路径**: /api/v1/groups
- **版本**: v1.0.0

---

## 目录
1. [创建群组](#1-创建群组)
2. [获取群组信息](#2-获取群组信息)
3. [更新群组信息](#3-更新群组信息)
4. [解散群组](#4-解散群组)
5. [添加成员](#5-添加成员)
6. [移除成员](#6-移除成员)
7. [获取成员列表](#7-获取成员列表)
8. [设置管理员](#8-设置管理员)

---

## 1. 创建群组

### 接口信息
- **URL**: `/`
- **Method**: `POST`
- **功能**: 创建新的群组
- **认证**: 需要 Bearer Token

### Request
```json
{
  "name": "技术交流群",
  "description": "讨论技术问题的群组",
  "avatar": "https://cdn.example.com/group-avatar.jpg",
  "maxMembers": 500,
  "joinType": "APPROVAL",
  "memberIds": [10002, 10003, 10004]
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 群组名称，2-30字符 |
| description | string | 否 | 群组描述 |
| avatar | string | 否 | 群组头像URL |
| maxMembers | integer | 否 | 最大成员数，默认500 |
| joinType | string | 否 | 加入方式：FREE/APPROVAL，默认FREE |
| memberIds | array | 否 | 初始成员ID列表 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "groupId": "group_001",
    "name": "技术交流群",
    "description": "讨论技术问题的群组",
    "avatar": "https://cdn.example.com/group-avatar.jpg",
    "ownerId": 10001,
    "memberCount": 4,
    "maxMembers": 500,
    "joinType": "APPROVAL",
    "createdAt": "2025-11-09T15:00:00Z"
  }
}
```

---

## 2. 获取群组信息

### 接口信息
- **URL**: `/{groupId}`
- **Method**: `GET`
- **功能**: 获取群组详细信息
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/groups/group_001
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "groupId": "group_001",
    "name": "技术交流群",
    "description": "讨论技术问题的群组",
    "avatar": "https://cdn.example.com/group-avatar.jpg",
    "ownerId": 10001,
    "ownerName": "张三",
    "memberCount": 50,
    "maxMembers": 500,
    "joinType": "APPROVAL",
    "announcement": "欢迎大家加入技术交流群",
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2025-11-09T15:00:00Z"
  }
}
```

---

## 3. 更新群组信息

### 接口信息
- **URL**: `/{groupId}`
- **Method**: `PUT`
- **功能**: 更新群组信息（仅群主和管理员）
- **认证**: 需要 Bearer Token

### Request
```json
{
  "name": "技术交流群（新）",
  "description": "更新后的描述",
  "avatar": "https://cdn.example.com/new-avatar.jpg",
  "announcement": "新的群公告",
  "joinType": "FREE"
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "groupId": "group_001",
    "name": "技术交流群（新）",
    "description": "更新后的描述",
    "updatedAt": "2025-11-09T15:30:00Z"
  }
}
```

---

## 4. 解散群组

### 接口信息
- **URL**: `/{groupId}`
- **Method**: `DELETE`
- **功能**: 解散群组（仅群主）
- **认证**: 需要 Bearer Token

### Request
```
DELETE /api/v1/groups/group_001
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "群组已解散",
  "data": null
}
```

---

## 5. 添加成员

### 接口信息
- **URL**: `/{groupId}/members`
- **Method**: `POST`
- **功能**: 添加新成员到群组
- **认证**: 需要 Bearer Token

### Request
```json
{
  "userIds": [10005, 10006, 10007],
  "inviterId": 10001
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "添加成功",
  "data": {
    "groupId": "group_001",
    "addedCount": 3,
    "failedUsers": [],
    "currentMemberCount": 53
  }
}
```

---

## 6. 移除成员

### 接口信息
- **URL**: `/{groupId}/members/{userId}`
- **Method**: `DELETE`
- **功能**: 移除群组成员（管理员权限）
- **认证**: 需要 Bearer Token

### Request
```
DELETE /api/v1/groups/group_001/members/10005
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "已移除成员",
  "data": {
    "groupId": "group_001",
    "removedUserId": 10005,
    "currentMemberCount": 52
  }
}
```

---

## 7. 获取成员列表

### 接口信息
- **URL**: `/{groupId}/members`
- **Method**: `GET`
- **功能**: 获取群组成员列表
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/groups/group_001/members?page=1&size=20&role=ALL
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页数量，默认20 |
| role | string | 否 | 角色筛选：ALL/OWNER/ADMIN/MEMBER |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 52,
    "page": 1,
    "size": 20,
    "members": [
      {
        "userId": 10001,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": "https://cdn.example.com/avatar/10001.jpg",
        "role": "OWNER",
        "joinedAt": "2025-01-01T00:00:00Z"
      },
      {
        "userId": 10002,
        "username": "lisi",
        "nickname": "李四",
        "avatar": "https://cdn.example.com/avatar/10002.jpg",
        "role": "ADMIN",
        "joinedAt": "2025-01-02T00:00:00Z"
      }
    ]
  }
}
```

---

## 8. 设置管理员

### 接口信息
- **URL**: `/{groupId}/admins`
- **Method**: `POST`
- **功能**: 设置或取消群管理员（仅群主）
- **认证**: 需要 Bearer Token

### Request
```json
{
  "userId": 10002,
  "action": "ADD"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | number | 是 | 用户ID |
| action | string | 是 | 操作：ADD/REMOVE |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "设置成功",
  "data": {
    "groupId": "group_001",
    "userId": 10002,
    "role": "ADMIN"
  }
}
```

---

## 群组角色

| 角色 | 权限 |
|------|------|
| OWNER | 群主，拥有所有权限 |
| ADMIN | 管理员，可管理成员、修改群信息 |
| MEMBER | 普通成员，可发送消息 |

---

## 加入方式

| 类型 | 说明 |
|------|------|
| FREE | 自由加入，无需审批 |
| APPROVAL | 需要管理员审批 |
| INVITE_ONLY | 仅邀请，不可搜索 |

---

## 数据模型

### Group
```typescript
interface Group {
  groupId: string;          // 群组ID
  name: string;             // 群组名称
  description: string;      // 群组描述
  avatar: string;           // 群组头像
  ownerId: number;          // 群主ID
  memberCount: number;      // 成员数量
  maxMembers: number;       // 最大成员数
  joinType: JoinType;       // 加入方式
  announcement: string;     // 群公告
  createdAt: string;        // 创建时间
  updatedAt: string;        // 更新时间
}

interface GroupMember {
  userId: number;           // 用户ID
  username: string;         // 用户名
  nickname: string;         // 昵称
  avatar: string;           // 头像
  role: MemberRole;         // 角色
  joinedAt: string;         // 加入时间
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-09  
**维护人**: 开发团队
