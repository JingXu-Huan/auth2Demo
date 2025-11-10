# 组织架构服务 API 文档

## 服务信息
- **服务名称**: organization-service
- **端口**: 8011
- **基础路径**: /api/v1/organization
- **版本**: v1.0.0

---

## 目录
1. [创建部门](#1-创建部门)
2. [获取部门信息](#2-获取部门信息)
3. [更新部门信息](#3-更新部门信息)
4. [删除部门](#4-删除部门)
5. [获取部门树](#5-获取部门树)
6. [添加部门成员](#6-添加部门成员)
7. [移除部门成员](#7-移除部门成员)
8. [获取部门成员列表](#8-获取部门成员列表)
9. [设置部门负责人](#9-设置部门负责人)
10. [获取组织通讯录](#10-获取组织通讯录)

---

## 1. 创建部门

### 接口信息
- **URL**: `/departments`
- **Method**: `POST`
- **功能**: 创建新部门
- **认证**: 需要 Bearer Token
- **权限**: 管理员

### Request
```json
{
  "name": "技术部",
  "parentId": null,
  "description": "负责技术研发",
  "leaderId": 10001,
  "order": 1
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 部门名称 |
| parentId | number | 否 | 父部门ID，null表示顶级部门 |
| description | string | 否 | 部门描述 |
| leaderId | number | 否 | 部门负责人ID |
| order | integer | 否 | 排序序号 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "departmentId": 1001,
    "name": "技术部",
    "parentId": null,
    "description": "负责技术研发",
    "leaderId": 10001,
    "leaderName": "张三",
    "memberCount": 0,
    "order": 1,
    "level": 1,
    "fullPath": "/技术部",
    "createdAt": "2025-11-10T10:00:00Z"
  }
}
```

---

## 2. 获取部门信息

### 接口信息
- **URL**: `/departments/{departmentId}`
- **Method**: `GET`
- **功能**: 获取部门详细信息
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/organization/departments/1001
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "departmentId": 1001,
    "name": "技术部",
    "parentId": null,
    "description": "负责技术研发",
    "leaderId": 10001,
    "leaderName": "张三",
    "leaderAvatar": "https://cdn.example.com/avatar/10001.jpg",
    "memberCount": 25,
    "childDepartments": [
      {
        "departmentId": 1002,
        "name": "前端组",
        "memberCount": 10
      },
      {
        "departmentId": 1003,
        "name": "后端组",
        "memberCount": 15
      }
    ],
    "order": 1,
    "level": 1,
    "fullPath": "/技术部",
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2025-11-10T10:00:00Z"
  }
}
```

---

## 3. 更新部门信息

### 接口信息
- **URL**: `/departments/{departmentId}`
- **Method**: `PUT`
- **功能**: 更新部门信息
- **认证**: 需要 Bearer Token
- **权限**: 管理员或部门负责人

### Request
```json
{
  "name": "技术研发部",
  "description": "负责产品技术研发",
  "leaderId": 10002,
  "order": 2
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "departmentId": 1001,
    "name": "技术研发部",
    "description": "负责产品技术研发",
    "leaderId": 10002,
    "updatedAt": "2025-11-10T11:00:00Z"
  }
}
```

---

## 4. 删除部门

### 接口信息
- **URL**: `/departments/{departmentId}`
- **Method**: `DELETE`
- **功能**: 删除部门（必须为空部门）
- **认证**: 需要 Bearer Token
- **权限**: 管理员

### Request
```
DELETE /api/v1/organization/departments/1001
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### Response - 失败
```json
{
  "code": 400,
  "message": "部门下还有成员，无法删除",
  "data": {
    "memberCount": 25
  }
}
```

---

## 5. 获取部门树

### 接口信息
- **URL**: `/departments/tree`
- **Method**: `GET`
- **功能**: 获取完整的部门树形结构
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/organization/departments/tree
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tree": [
      {
        "departmentId": 1001,
        "name": "技术部",
        "parentId": null,
        "leaderId": 10001,
        "leaderName": "张三",
        "memberCount": 25,
        "order": 1,
        "level": 1,
        "children": [
          {
            "departmentId": 1002,
            "name": "前端组",
            "parentId": 1001,
            "leaderId": 10002,
            "leaderName": "李四",
            "memberCount": 10,
            "order": 1,
            "level": 2,
            "children": []
          },
          {
            "departmentId": 1003,
            "name": "后端组",
            "parentId": 1001,
            "leaderId": 10003,
            "leaderName": "王五",
            "memberCount": 15,
            "order": 2,
            "level": 2,
            "children": []
          }
        ]
      },
      {
        "departmentId": 2001,
        "name": "产品部",
        "parentId": null,
        "leaderId": 10010,
        "leaderName": "赵六",
        "memberCount": 15,
        "order": 2,
        "level": 1,
        "children": []
      }
    ]
  }
}
```

---

## 6. 添加部门成员

### 接口信息
- **URL**: `/departments/{departmentId}/members`
- **Method**: `POST`
- **功能**: 添加成员到部门
- **认证**: 需要 Bearer Token
- **权限**: 管理员或部门负责人

### Request
```json
{
  "userIds": [10005, 10006, 10007],
  "position": "工程师",
  "isMain": true
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userIds | array | 是 | 用户ID列表 |
| position | string | 否 | 职位 |
| isMain | boolean | 否 | 是否主部门，默认true |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "添加成功",
  "data": {
    "departmentId": 1001,
    "addedCount": 3,
    "failedUsers": [],
    "currentMemberCount": 28
  }
}
```

---

## 7. 移除部门成员

### 接口信息
- **URL**: `/departments/{departmentId}/members/{userId}`
- **Method**: `DELETE`
- **功能**: 从部门移除成员
- **认证**: 需要 Bearer Token
- **权限**: 管理员或部门负责人

### Request
```
DELETE /api/v1/organization/departments/1001/members/10005
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "移除成功",
  "data": {
    "departmentId": 1001,
    "removedUserId": 10005,
    "currentMemberCount": 27
  }
}
```

---

## 8. 获取部门成员列表

### 接口信息
- **URL**: `/departments/{departmentId}/members`
- **Method**: `GET`
- **功能**: 获取部门成员列表
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/organization/departments/1001/members?page=1&size=20&includeSubDepartments=false
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页数量，默认20 |
| includeSubDepartments | boolean | 否 | 是否包含子部门成员，默认false |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 25,
    "page": 1,
    "size": 20,
    "members": [
      {
        "userId": 10001,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": "https://cdn.example.com/avatar/10001.jpg",
        "email": "zhangsan@example.com",
        "phone": "138****8000",
        "position": "技术总监",
        "isLeader": true,
        "isMain": true,
        "joinedAt": "2025-01-01T00:00:00Z",
        "status": "ONLINE"
      },
      {
        "userId": 10002,
        "username": "lisi",
        "nickname": "李四",
        "avatar": "https://cdn.example.com/avatar/10002.jpg",
        "email": "lisi@example.com",
        "phone": "139****9000",
        "position": "前端工程师",
        "isLeader": false,
        "isMain": true,
        "joinedAt": "2025-01-15T00:00:00Z",
        "status": "ONLINE"
      }
    ]
  }
}
```

---

## 9. 设置部门负责人

### 接口信息
- **URL**: `/departments/{departmentId}/leader`
- **Method**: `PUT`
- **功能**: 设置或更换部门负责人
- **认证**: 需要 Bearer Token
- **权限**: 管理员

### Request
```json
{
  "leaderId": 10002
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "设置成功",
  "data": {
    "departmentId": 1001,
    "leaderId": 10002,
    "leaderName": "李四",
    "updatedAt": "2025-11-10T12:00:00Z"
  }
}
```

---

## 10. 获取组织通讯录

### 接口信息
- **URL**: `/contacts`
- **Method**: `GET`
- **功能**: 获取组织通讯录（按部门分组）
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/organization/contacts?keyword=张&departmentId=1001
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词（姓名/手机/邮箱） |
| departmentId | number | 否 | 部门ID筛选 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "departments": [
      {
        "departmentId": 1001,
        "name": "技术部",
        "fullPath": "/技术部",
        "members": [
          {
            "userId": 10001,
            "username": "zhangsan",
            "nickname": "张三",
            "avatar": "https://cdn.example.com/avatar/10001.jpg",
            "email": "zhangsan@example.com",
            "phone": "138****8000",
            "position": "技术总监",
            "status": "ONLINE"
          }
        ]
      }
    ]
  }
}
```

---

## 数据模型

### Department
```typescript
interface Department {
  departmentId: number;       // 部门ID
  name: string;               // 部门名称
  parentId: number | null;    // 父部门ID
  description: string;        // 部门描述
  leaderId: number;           // 负责人ID
  leaderName: string;         // 负责人姓名
  memberCount: number;        // 成员数量
  order: number;              // 排序序号
  level: number;              // 层级（1为顶级）
  fullPath: string;           // 完整路径
  createdAt: string;          // 创建时间
  updatedAt: string;          // 更新时间
  children?: Department[];    // 子部门列表
}

interface DepartmentMember {
  userId: number;             // 用户ID
  username: string;           // 用户名
  nickname: string;           // 昵称
  avatar: string;             // 头像
  email: string;              // 邮箱
  phone: string;              // 手机号
  position: string;           // 职位
  isLeader: boolean;          // 是否负责人
  isMain: boolean;            // 是否主部门
  joinedAt: string;           // 加入时间
  status: UserStatus;         // 在线状态
}
```

---

## 权限说明

| 操作 | 管理员 | 部门负责人 | 普通成员 |
|------|--------|-----------|---------|
| 创建部门 | ✅ | ❌ | ❌ |
| 删除部门 | ✅ | ❌ | ❌ |
| 更新部门信息 | ✅ | ✅ | ❌ |
| 添加成员 | ✅ | ✅ | ❌ |
| 移除成员 | ✅ | ✅ | ❌ |
| 查看部门信息 | ✅ | ✅ | ✅ |
| 查看通讯录 | ✅ | ✅ | ✅ |

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-10  
**维护人**: 开发团队
