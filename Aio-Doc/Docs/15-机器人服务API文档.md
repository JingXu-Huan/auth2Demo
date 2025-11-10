# 机器人服务 API 文档

## 服务信息
- **服务名称**: bot-service
- **端口**: 8015
- **基础路径**: /api/v1/bots
- **版本**: v1.0.0

---

## 📋 服务功能说明

### 核心功能
机器人服务提供智能机器人创建、管理、消息处理等功能，支持自定义机器人、webhook集成、自动化工作流等企业级需求。

### 主要特性

#### 1. 机器人管理
- ✅ **创建机器人**: 自定义机器人
- ✅ **机器人配置**: 设置名称、头像、描述
- ✅ **权限管理**: 控制机器人权限
- ✅ **机器人启用/禁用**: 控制机器人状态

#### 2. 消息处理
- ✅ **接收消息**: 接收用户消息
- ✅ **发送消息**: 主动发送消息
- ✅ **消息卡片**: 富文本消息
- ✅ **交互式消息**: 按钮、表单等

#### 3. Webhook集成
- ✅ **Webhook配置**: 配置回调地址
- ✅ **事件订阅**: 订阅消息事件
- ✅ **签名验证**: 安全验证

#### 4. 自动化
- ✅ **定时任务**: 定时发送消息
- ✅ **关键词回复**: 自动回复
- ✅ **工作流**: 自动化流程

---

## 1. 创建机器人

### Request
```json
{
  "name": "通知机器人",
  "description": "系统通知机器人",
  "avatar": "https://cdn.example.com/bot-avatar.png",
  "webhookUrl": "https://api.example.com/webhook",
  "abilities": ["SEND_MESSAGE", "RECEIVE_MESSAGE"]
}
```

### Response
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "botId": "bot_123456",
    "name": "通知机器人",
    "appId": "app_xxx",
    "appSecret": "secret_xxx",
    "webhookUrl": "https://api.example.com/webhook",
    "createdAt": "2025-11-10T12:00:00Z"
  }
}
```

---

## 2. 发送消息

### Request
```json
{
  "chatType": "PRIVATE",
  "receiverId": 10001,
  "messageType": "CARD",
  "content": {
    "title": "系统通知",
    "description": "您有新的待办任务",
    "buttons": [
      {
        "text": "查看详情",
        "url": "https://example.com/task/123"
      }
    ]
  }
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-11  
**维护人**: 开发团队
