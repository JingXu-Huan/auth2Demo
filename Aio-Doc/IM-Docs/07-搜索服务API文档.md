# 搜索服务 API 文档

## 服务信息
- **服务名称**: search-service
- **端口**: 8007
- **基础路径**: /api/v1/search
- **版本**: v1.0.0

---

## 核心接口

### 1. 搜索消息
- **URL**: `/messages`
- **Method**: `GET`
- **功能**: 全文搜索聊天消息

**Request**:
```
GET /api/v1/search/messages?keyword=技术&userId=10001&page=1&size=20
```

**参数说明**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 是 | 搜索关键词 |
| userId | number | 是 | 用户ID |
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页数量，默认20 |

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 15,
    "page": 1,
    "size": 20,
    "results": [
      {
        "messageId": "msg_123456",
        "content": "讨论技术问题",
        "senderId": 10002,
        "senderName": "李四",
        "chatType": "PRIVATE",
        "timestamp": "2025-11-09T14:00:00Z",
        "highlight": "讨论<em>技术</em>问题"
      }
    ]
  }
}
```

---

### 2. 搜索用户
- **URL**: `/users`
- **Method**: `GET`
- **功能**: 搜索用户

**Request**:
```
GET /api/v1/search/users?keyword=张三&page=1&size=20
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "size": 20,
    "results": [
      {
        "userId": 10001,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": "https://cdn.example.com/avatar/10001.jpg",
        "signature": "热爱技术",
        "isFriend": false
      }
    ]
  }
}
```

---

### 3. 搜索群组
- **URL**: `/groups`
- **Method**: `GET`
- **功能**: 搜索群组

**Request**:
```
GET /api/v1/search/groups?keyword=技术交流&page=1&size=20
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 3,
    "page": 1,
    "size": 20,
    "results": [
      {
        "groupId": "group_001",
        "name": "技术交流群",
        "description": "讨论技术问题的群组",
        "avatar": "https://cdn.example.com/group-avatar.jpg",
        "memberCount": 50,
        "joinType": "FREE",
        "isMember": false
      }
    ]
  }
}
```

---

### 4. 综合搜索
- **URL**: `/all`
- **Method**: `GET`
- **功能**: 搜索所有类型（用户、群组、消息）

**Request**:
```
GET /api/v1/search/all?keyword=技术&userId=10001
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "users": {
      "total": 5,
      "results": [...]
    },
    "groups": {
      "total": 3,
      "results": [...]
    },
    "messages": {
      "total": 15,
      "results": [...]
    }
  }
}
```

---

### 5. 搜索建议
- **URL**: `/suggest`
- **Method**: `GET`
- **功能**: 获取搜索建议（自动补全）

**Request**:
```
GET /api/v1/search/suggest?keyword=技&type=USER
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "suggestions": [
      "技术交流群",
      "技术分享",
      "技术讨论"
    ]
  }
}
```

---

## 搜索类型

| 类型 | 说明 |
|------|------|
| USER | 用户搜索 |
| GROUP | 群组搜索 |
| MESSAGE | 消息搜索 |
| ALL | 综合搜索 |

---

## 搜索特性

- ✅ 全文搜索
- ✅ 模糊匹配
- ✅ 高亮显示
- ✅ 分页支持
- ✅ 搜索建议
- ✅ 搜索历史

---

## 数据模型

```typescript
interface SearchResult<T> {
  total: number;
  page: number;
  size: number;
  results: T[];
}

interface MessageSearchResult {
  messageId: string;
  content: string;
  senderId: number;
  senderName: string;
  chatType: 'PRIVATE' | 'GROUP';
  timestamp: string;
  highlight: string;
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-09  
**维护人**: 开发团队
