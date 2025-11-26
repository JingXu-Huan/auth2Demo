

作为微服务架构中的“安全基石”，`auth-service` 不仅负责简单的登录注册，更承载了**多因子认证、会话生命周期管理、第三方生态接入以及核心风控策略执行**的重任。

---

### 1. 核心定位与职责边界
+ **身份提供者 (IdP)**：统一管理用户身份（User）、凭证（Credentials）和角色（Roles）。
+ **令牌工厂 (Token Factory)**：负责 JWT Access Token 的签发、Refresh Token 的管理与轮转。
+ **生态连接器 (Connector)**：对接 Gitee、GitHub、微信等 OAuth2 协议。
+ **风控执行器 (Enforcer)**：执行封号、禁言、踢下线等强制策略，并实时同步给网关和 IM。

---

### 2. 技术选型与依赖
+ **核心框架**: Spring Boot 3.x
+ **安全框架**: Spring Security (仅用于密码加密与内部链式防护，认证逻辑自定义)
+ **ORM**: MyBatis Plus (操作 `users`, `user_auths` 等表)
+ **缓存**: Redis Cluster (存储 Refresh Token、验证码、黑名单)
+ **消息队列**: RocketMQ (发送邮件、广播风控事件)
+ **工具库**:
    - **JJWT / Jose4j**: JWT 生成与解析。
    - **JustAuth**: 开箱即用的第三方 OAuth 登录工具库。
    - **Google Authenticator**: (可选) MFA 双因素认证。

---

### 3. 核心 API 接口设计
遵循 RESTful 风格，主要分为三个域：认证、设备、管理。

#### 3.1 认证域 (Authentication)
| 方法 | 路径 | 描述 | 关键参数 |
| :--- | :--- | :--- | :--- |
| POST | `/api/auth/code/send` | 发送邮箱验证码 | `email`, `type` (register/login) |
| POST | `/api/auth/register` | 邮箱验证码注册 | `email`, `code`, `password`, `nickname` |
| POST | `/api/auth/login/email` | 邮箱密码登录 | `email`, `password`, `device_info` |
| POST | `/api/auth/login/gitee` | Gitee 授权登录 | `code`, `device_info` |
| POST | `/api/auth/token/refresh` | 刷新 Token (续期) | `refresh_token` |
| POST | `/api/auth/logout` | 退出登录 | `refresh_token` |


#### 3.2 设备与安全域 (Device & Security)
| 方法 | 路径 | 描述 | 关键参数 |
| :--- | :--- | :--- | :--- |
| GET | `/api/auth/devices` | 获取当前登录设备列表 | (Header Token) |
| DELETE | `/api/auth/devices/{deviceId}` | 踢掉特定设备 | `deviceId` |
| POST | `/api/auth/password/reset` | 重置密码 | `old_pwd`, `new_pwd` |


#### 3.3 管理员风控域 (Admin Governance)
_仅限拥有 _`ADMIN`_ 角色的 Token 调用_

| 方法 | 路径 | 描述 | 关键参数 |
| :--- | :--- | :--- | :--- |
| POST | `/api/admin/user/ban` | 封禁用户 | `userId`, `duration`, `reason` |
| POST | `/api/admin/user/unban` | 解封用户 | `userId` |


---

### 4. 核心业务流程与逻辑实现
#### 4.1 双 Token 机制 (核心会话管理)
为了平衡安全性和用户体验，采用 **Short-lived Access Token + Long-lived Refresh Token** 策略。

+ **Access Token (JWT)**
    - **载荷**: `uid`, `role`, `deviceId`, `exp` (15分钟)。
    - **存储**: 客户端内存/Local Storage。
    - **验证**: 网关无状态本地验签。
+ **Refresh Token (UUID)**
    - **载荷**: 无（只是一个随机字符串）。
    - **存储**:
        * **Redis**: Key=`auth:refresh:{token}`, Value=`userId`, TTL=7天。
        * **客户端**: HttpOnly Cookie 或 Secure Storage。
    - **作用**: 当 Access Token 过期 (401) 时，前端静默调用 `/refresh` 接口换取新的一对 Token。

#### 4.2 Gitee 登录与自动绑定流程
利用 `JustAuth` 简化 OAuth 流程，实现“存在即登录，不存在即注册”。

```java
public AuthResponse loginByGitee(String code, DeviceInfo device) {
    // 1. 换取 Gitee 用户信息
    AuthUser giteeUser = justAuthRequest.getUser(AuthUser.builder().code(code).build());
    String openId = giteeUser.getUuid();

    // 2. 查询绑定关系
    UserAuth auth = userAuthRepo.findByTypeAndId("GITEE", openId);

    Long userId;
    if (auth != null) {
        // 3a. 老用户：直接获取 ID
        userId = auth.getUserId();
    } else {
        // 3b. 新用户：自动注册 (事务)
        userId = snowflake.nextId();
        User newUser = new User(userId, giteeUser.getNickname(), giteeUser.getAvatar(), ...);
        UserAuth newAuth = new UserAuth(userId, "GITEE", openId, giteeUser.getToken().getAccessToken());
        
        userRepo.save(newUser);
        userAuthRepo.save(newAuth);
        
        // 发送 MQ 事件 (异步同步到 Neo4j)
        rocketMQ.send("USER_EVENT", new UserCreatedEvent(userId, ...));
    }

    // 4. 执行通用登录逻辑 (生成 Token, 记录设备)
    return generateTokens(userId, device);
}
```

#### 4.3 设备管理与踢下线 (Kick-out Logic)
这是企业级安全的重要一环。

+ **登录时**:
    1. `upsert` 数据库表 `user_devices` (根据 userId + deviceId)。
    2. 如果策略是“单端登录”，则需先查找该用户旧的 deviceId，执行踢下线逻辑。
+ **主动踢下线**:

```java
public void kickDevice(Long userId, String deviceId) {
    // 1. 数据库标记为下线
    userDeviceRepo.updateStatus(userId, deviceId, Status.KICKED);

    // 2. Redis 写入踢人标记 (网关鉴权时会查)
    // Key: auth:kick:{userId}:{deviceId}, Value: 1, TTL: 15min (即Access Token剩余有效期)
    redisTemplate.opsForValue().set("auth:kick:" + userId + ":" + deviceId, "1", 15, TimeUnit.MINUTES);

    // 3. 删除该设备关联的 Refresh Token (防止它刷新)
    // 这需要维护一个 Mapping: userId:deviceId -> refresh_token
    redisTemplate.delete(getRefreshTokenKey(userId, deviceId));

    // 4. 发送 MQ 消息 (通知 IM 网关断开长连接)
    rocketMQ.send("RISK_EVENT", new KickEvent(userId, deviceId));
}
```

#### 4.4 封禁用户 (Ban User)
当管理员封禁用户时，需要达到“秒级生效”的效果。

1. **DB 落库**: 插入 `user_punishments` 表，记录原因、时间。
2. **Redis 封禁**: 写入 `risk:ban:user:{userId}`，TTL = 封禁时长。
    - _Gateway_ 会拦截所有 HTTP 请求。
3. **清除会话**: 删除该用户下**所有设备**的 Refresh Token。
4. **广播事件**: 发送 MQ `USER_BANNED` 事件。
    - _IM-Service_ 消费该事件，断开 WebSocket 连接。

---

### 5. 关键数据库交互 (Repository Layer)
使用 MyBatis Plus 简化操作。

```java
// UserAuthMapper.java
@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuth> {
    // 联合查询：登录时同时获取 User 基础信息和 Auth 信息
    @Select("SELECT u.*, a.credential FROM users u " +
            "LEFT JOIN user_auths a ON u.user_id = a.user_id " +
            "WHERE a.identity_type = #{type} AND a.identifier = #{identifier}")
    UserLoginDTO selectUserByAuth(@Param("type") String type, 
                                  @Param("identifier") String id);
}
```

---

### 6. 安全性加固措施
1. **密码存储**: 使用 `BCryptPasswordEncoder`。
    - 存储格式: `$2a$10$r.8...` (包含盐值)。
2. **验证码防刷**:
    - **频率限制**: 同一邮箱 1分钟只能发 1 次。
    - **IP 限制**: 同一 IP 24小时只能发 20 次。
    - Redis Key: `rate:limit:email:{email}`。
3. **敏感信息脱敏**:
    - Token 中不要放入手机号、邮箱等隐私信息，只放 `userId`。
    - 日志中隐藏 `password` 和 `token`。

---

### 7. 与其他模块的联动 (RocketMQ)
`auth-service` 作为生产者，产生以下核心事件：

+ **Topic**: `USER_EVENT`
    - **Tag**: `CREATED` -> 消费者: `sync-worker` (同步到 Neo4j), `user-service` (初始化默认部门)。
    - **Tag**: `UPDATED` -> 消费者: `im-service` (更新冗余的用户头像缓存)。
+ **Topic**: `RISK_EVENT`
    - **Tag**: `KICK_OUT` -> 消费者: `im-gateway` (关闭对应的 Netty Channel)。
    - **Tag**: `BAN` -> 消费者: `im-gateway` (全端断连)。
+ **Topic**: `EMAIL_SEND` (解耦邮件发送，防止 SMTP 阻塞主线程)。

### 8. 总结
`auth-service` 的设计核心在于**“状态的分层”**：

+ **JWT** 处理高频无状态鉴权。
+ **Redis** 处理会话续期和实时风控拦截。
+ **Database** 存储最终的身份与审计记录。

这个设计完全支撑了你之前提出的“多端登录”、“Gitee 集成”以及“深度风控”的需求。

