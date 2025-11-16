# 用户服务 API 文档

## 📋 服务功能说明

用户服务是整个IM系统的核心基础服务，负责用户账户的全生命周期管理。本服务提供完整的用户认证、授权、信息管理和社交关系管理功能，是所有其他服务的基础依赖。

### 核心功能

#### 1. 用户账户管理
- **用户注册**: 支持邮箱注册，包含邮箱验证、密码强度校验
- **用户登录**: 支持邮箱密码登录、第三方OAuth登录（Gitee/GitHub）
- **账户安全**: 登录失败锁定、长期未登录验证、密码历史防重复
- **多设备管理**: 支持多设备同时在线，设备信息追踪

#### 2. 用户信息管理
- **基础信息**: 用户名、昵称、头像、个性签名、性别、生日、所在地
- **联系方式**: 邮箱、手机号（支持验证）
- **在线状态**: ONLINE（在线）、OFFLINE（离线）、BUSY（忙碌）、AWAY（离开）
- **隐私设置**: 信息可见性控制、好友验证设置

#### 3. 社交关系管理
- **好友系统**: 添加好友、删除好友、好友列表、好友备注
- **好友申请**: 申请发送、申请处理（同意/拒绝）、申请消息
- **好友来源**: 搜索添加、二维码、手机号、群组
- **黑名单**: 拉黑用户、解除拉黑

#### 4. 用户搜索
- **精确搜索**: 通过用户名、邮箱、手机号精确查找
- **模糊搜索**: 支持昵称、用户名模糊匹配
- **高级筛选**: 按地区、性别、在线状态等条件筛选

#### 5. 安全与审计
- **登录日志**: 记录所有登录行为（成功/失败）、IP地址、设备信息
- **密码管理**: BCrypt加密、密码历史记录、定期更换提醒
- **账户状态**: 启用/禁用、锁定/解锁、凭证过期管理
- **邮箱验证**: 注册验证、找回密码、敏感操作二次验证

### 技术特性
- **认证方式**: OAuth2 + JWT Token
- **数据库**: PostgreSQL（主库）+ Redis（缓存）
- **消息队列**: RabbitMQ（异步任务）
- **文件存储**: OSS（头像、附件）
- **服务发现**: Nacos
- **负载均衡**: Ribbon
- **熔断降级**: Sentinel

---

## 服务信息
- **服务名称**: user-service
- **端口**: 8001
- **基础路径**: /api/v1/users
- **版本**: v1.0.0

---

## 目录
1. [用户注册](#1-用户注册)
2. [用户登录](#2-用户登录)
3. [获取用户信息](#3-获取用户信息)
4. [更新用户信息](#4-更新用户信息)
5. [搜索用户](#5-搜索用户)
6. [添加好友](#6-添加好友)
7. [获取好友列表](#7-获取好友列表)
8. [删除好友](#8-删除好友)

---

## 1. 用户注册

### 接口信息
- **URL**: `/register`
- **Method**: `POST`
- **功能**: 用户注册，创建新账号
- **认证**: 不需要

### Request
```json
{
  "username": "zhangsan",
  "password": "123456",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "nickname": "张三"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，3-20字符，字母数字下划线 |
| password | string | 是 | 密码，6-20字符 |
| email | string | 是 | 邮箱地址 |
| phone | string | 是 | 手机号码 |
| nickname | string | 否 | 昵称，默认为用户名 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 10001,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "https://cdn.example.com/default-avatar.png",
    "email": "zhangsan@example.com",
    "phone": "138****8000",
    "createdAt": "2025-11-09T14:30:00Z"
  }
}
```

### Response - 失败
```json
{
  "code": 400,
  "message": "用户名已存在",
  "data": null
}
```

**错误码**
| 错误码 | 说明 |
|--------|------|
| 400 | 用户名已存在 |
| 400 | 邮箱已被注册 |
| 400 | 手机号已被注册 |
| 400 | 参数格式错误 |

---

## 2. 用户登录

### 接口信息
- **URL**: `/login`
- **Method**: `POST`
- **功能**: 用户登录，获取访问令牌
- **认证**: 不需要

### Request
```json
{
  "username": "zhangsan",
  "password": "123456",
  "deviceType": "WEB",
  "deviceId": "device-uuid-123"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名或邮箱或手机号 |
| password | string | 是 | 密码 |
| deviceType | string | 是 | 设备类型：WEB/IOS/ANDROID/PC |
| deviceId | string | 是 | 设备唯一标识 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": 10001,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "https://cdn.example.com/avatar/10001.jpg",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEwMDAxLCJleHAiOjE2OTk1MjA0MDB9.xxx",
    "refreshToken": "refresh_token_xxx",
    "expiresIn": 7200,
    "tokenType": "Bearer"
  }
}
```

### Response - 失败
```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

---

## 3. 获取用户信息

### 接口信息
- **URL**: `/{userId}`
- **Method**: `GET`
- **功能**: 获取指定用户的详细信息
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/users/10001
Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 10001,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "https://cdn.example.com/avatar/10001.jpg",
    "email": "zhangsan@example.com",
    "phone": "138****8000",
    "status": "ONLINE",
    "signature": "这是我的个性签名",
    "gender": "MALE",
    "birthday": "1990-01-01",
    "location": "北京市",
    "createdAt": "2025-01-01T00:00:00Z",
    "lastLoginAt": "2025-11-09T14:30:00Z"
  }
}
```

---

## 4. 更新用户信息

### 接口信息
- **URL**: `/{userId}`
- **Method**: `PUT`
- **功能**: 更新用户资料
- **认证**: 需要 Bearer Token

### Request
```json
{
  "nickname": "新昵称",
  "avatar": "https://cdn.example.com/avatar/new.jpg",
  "signature": "新的个性签名",
  "gender": "MALE",
  "birthday": "1990-01-01",
  "location": "上海市"
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "userId": 10001,
    "nickname": "新昵称",
    "avatar": "https://cdn.example.com/avatar/new.jpg",
    "signature": "新的个性签名",
    "updatedAt": "2025-11-09T15:00:00Z"
  }
}
```

---

## 5. 搜索用户

### 接口信息
- **URL**: `/search`
- **Method**: `GET`
- **功能**: 根据关键词搜索用户
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/users/search?keyword=张&page=1&size=20
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 是 | 搜索关键词（用户名/昵称/手机号） |
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页数量，默认20，最大100 |

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 20,
    "users": [
      {
        "userId": 10001,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": "https://cdn.example.com/avatar/10001.jpg",
        "status": "ONLINE",
        "signature": "个性签名"
      }
    ]
  }
}
```

---

## 6. 添加好友

### 接口信息
- **URL**: `/friends`
- **Method**: `POST`
- **功能**: 发送好友申请
- **认证**: 需要 Bearer Token

### Request
```json
{
  "friendId": 10002,
  "message": "你好，我是张三，想加你为好友",
  "source": "SEARCH",
  "remark": "同事"
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "好友申请已发送",
  "data": {
    "requestId": "req_123456",
    "status": "PENDING",
    "createdAt": "2025-11-09T15:00:00Z"
  }
}
```

---

## 7. 获取好友列表

### 接口信息
- **URL**: `/friends`
- **Method**: `GET`
- **功能**: 获取当前用户的好友列表
- **认证**: 需要 Bearer Token

### Request
```
GET /api/v1/users/friends?status=ACCEPTED
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "friends": [
      {
        "userId": 10002,
        "username": "lisi",
        "nickname": "李四",
        "avatar": "https://cdn.example.com/avatar/10002.jpg",
        "status": "ONLINE",
        "remark": "同事",
        "addedAt": "2025-01-15T10:00:00Z"
      }
    ]
  }
}
```

---

## 8. 删除好友

### 接口信息
- **URL**: `/friends/{friendId}`
- **Method**: `DELETE`
- **功能**: 删除好友关系
- **认证**: 需要 Bearer Token

### Request
```
DELETE /api/v1/users/friends/10002
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "已删除好友",
  "data": null
}
```

---

## 通用错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（Token无效或过期） |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

---

## 数据模型

### User
```typescript
interface User {
  userId: number;           // 用户ID
  username: string;         // 用户名
  nickname: string;         // 昵称
  avatar: string;           // 头像URL
  email: string;            // 邮箱
  phone: string;            // 手机号（脱敏）
  status: UserStatus;       // 在线状态
  signature: string;        // 个性签名
  gender: Gender;           // 性别
  birthday: string;         // 生日
  location: string;         // 所在地
  createdAt: string;        // 创建时间
  updatedAt: string;        // 更新时间
  lastLoginAt: string;      // 最后登录时间
}

enum UserStatus {
  ONLINE = 'ONLINE',        // 在线
  OFFLINE = 'OFFLINE',      // 离线
  BUSY = 'BUSY',            // 忙碌
  AWAY = 'AWAY'             // 离开
}

enum Gender {
  MALE = 'MALE',            // 男
  FEMALE = 'FEMALE',        // 女
  UNKNOWN = 'UNKNOWN'       // 未知
}
```

---

## 调用示例

### JavaScript/TypeScript
```javascript
// 登录
const login = async (username, password) => {
  const response = await fetch('http://localhost:8001/api/v1/users/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      username,
      password,
      deviceType: 'WEB',
      deviceId: 'web-' + Date.now()
    })
  });
  
  const result = await response.json();
  if (result.code === 200) {
    localStorage.setItem('token', result.data.token);
    return result.data;
  }
  throw new Error(result.message);
};

// 获取用户信息
const getUserInfo = async (userId) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`http://localhost:8001/api/v1/users/${userId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const result = await response.json();
  return result.data;
};
```

### Java
```java
// 使用OpenFeign调用
@FeignClient(name = "user-service", url = "http://localhost:8001")
public interface UserServiceClient {
    
    @PostMapping("/api/v1/users/login")
    Result<LoginResponse> login(@RequestBody LoginRequest request);
    
    @GetMapping("/api/v1/users/{userId}")
    Result<User> getUserInfo(@PathVariable Long userId,
                             @RequestHeader("Authorization") String token);
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-09  
**维护人**: 开发团队
