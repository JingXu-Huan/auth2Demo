# Gateway Sentinel Nacos 配置指南

##  Nacos 配置步骤

### 1. 登录 Nacos 控制台
访问: http://154.219.109.125:8848/nacos
用户名/密码: nacos/nacos

### 2. 创建流控规则配置

**配置信息**:
- Data ID: gateway-flow-rules
- Group: SENTINEL_GROUP
- 配置格式: JSON

**配置内容**:
```json
[
  {
    \"resource\": \"auth-server\",
    \"resourceMode\": 0,
    \"grade\": 1,
    \"count\": 50,
    \"intervalSec\": 1,
    \"controlBehavior\": 0,
    \"burst\": 0,
    \"maxQueueingTimeoutMs\": 500
  },
  {
    \"resource\": \"user-register\",
    \"resourceMode\": 0,
    \"grade\": 1,
    \"count\": 10,
    \"intervalSec\": 1,
    \"controlBehavior\": 0,
    \"burst\": 0,
    \"maxQueueingTimeoutMs\": 500
  },
  {
    \"resource\": \"user-confirm\",
    \"resourceMode\": 0,
    \"grade\": 1,
    \"count\": 20,
    \"intervalSec\": 1,
    \"controlBehavior\": 0,
    \"burst\": 0,
    \"maxQueueingTimeoutMs\": 500
  },
  {
    \"resource\": \"security-verification\",
    \"resourceMode\": 0,
    \"grade\": 1,
    \"count\": 30,
    \"intervalSec\": 1,
    \"controlBehavior\": 0,
    \"burst\": 0,
    \"maxQueueingTimeoutMs\": 500
  },
  {
    \"resource\": \"user-server\",
    \"resourceMode\": 0,
    \"grade\": 1,
    \"count\": 100,
    \"intervalSec\": 1,
    \"controlBehavior\": 0,
    \"burst\": 0,
    \"maxQueueingTimeoutMs\": 500
  }
]
```

##  规则说明

| 路由 | QPS 限制 | 说明 |
|------|---------|------|
| auth-server | 50 | OAuth2 认证接口 |
| user-register | 10 | 用户注册接口 |
| user-confirm | 20 | 邮箱验证接口 |
| security-verification | 30 | 安全验证接口 |
| user-server | 100 | 用户服务接口 |

##  验证

1. 启动 Gateway 服务
2. 查看日志: \"Gateway Sentinel 配置初始化完成（流控规则由 Nacos 动态管理）\"
3. 访问 Sentinel 控制台查看规则
4. 在 Nacos 修改规则，实时生效

