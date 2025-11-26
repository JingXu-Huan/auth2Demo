它是整个协作平台的**心脏**，承担着“高并发写入”、“严格消息有序”、“海量数据存储”和“数据最终一致性”的重任。它不直接维护长连接（那是 `im-gateway` 的事），而是专注于**消息的生命周期管理**。

---

### 1. 核心定位与设计哲学
+ **职责边界**：
    - **Write（写）**: 处理发送消息请求，保证数据落库（DB）与消息投递（MQ）的原子性。
    - **Read（读）**: 处理历史消息拉取（Sync），根据会话类型智能选择查询 Inbox 还是 Timeline。
    - **Control（控）**: 会话管理（建群、拉人）、未读数管理、消息撤回/修改。
+ **设计哲学**：
    - **重写轻读**: 消息发送链路极度优化，使用 MQ 削峰。
    - **严格有序**: 也就是 **Sequence ID** 机制，这是客户端实现“不丢、不重、不乱”的基石。
    - **冷热分离**: 利用 PostgreSQL 分区表特性，确保最近 1 个月的消息查询极快。

---

### 2. 架构分层设计
```mermaid
graph TD
    API[API Layer (Controller)] --> Biz[Business Layer]
    
    subgraph Business Layer
        SeqMgr[Sequence Manager\n(Redis Lua)]
        MsgDispatcher[Message Dispatcher\n(Push vs Pull Strategy)]
        GroupMgr[Group Manager]
    end
    
    Biz --> Data[Data Access Layer]
    
    subgraph Data Access Layer
        TimelineRepo[Timeline Repository]
        InboxRepo[Inbox Repository]
        SessionRepo[Session Repository]
    end
    
    Biz --> MQ[MQ Transaction Producer]
    
    Data -.-> PG[(PostgreSQL Partitioned)]
    SeqMgr -.-> Redis[(Redis Cluster)]
    MQ -.-> RocketMQ[(RocketMQ)]
```

---

### 3. 核心功能模块详细设计
#### 3.1 序号生成器 (Sequence Manager)
**目标**: 为每个 `channel_id` 生成严格单调递增的 `seq_id`。  
**原理**: 客户端不依赖时间戳排序，只依赖 `seq_id`。

+ **实现方案**: Redis `INCR` + Lua 脚本。
+ **Lua 脚本 (保证原子性)**:

```lua
-- key: channel:seq:{channel_id}
-- increment sequence
local seq = redis.call('INCR', KEYS[1])
return seq
```

+ **持久化**: 虽然 Redis 是内存数据库，但由于我们使用 AOF 持久化，且 seq_id 主要是为了排序和补洞。即使 Redis 挂了并丢失少量 seq（例如回拨），只要新生成的 seq 大于旧落库的 seq 即可，中间的空洞（Gap）会被客户端的 Gap Detection 机制识别为“无消息”。

#### 3.2 智能投递分发器 (Message Dispatcher)
这是你在《研究报告》中提到的**“混合架构”**的落地实现。

+ **逻辑流程**:
    1. 获取群成员数量 `N`。
    2. 判断阈值 (如 `N < 500`)。
    3. **模式 A (小群/单聊 - 写扩散)**:
        * 写入 `chat_messages` (Timeline)。
        * 批量写入 `chat_inbox` (为每个成员写一条引用)。
        * **优势**: 接收方上线 sync 时，直接查自己的 inbox，速度极快。
    4. **模式 B (大群 - 读扩散)**:
        * 仅写入 `chat_messages` (Timeline)。
        * **优势**: 发送方无压力，避免写放大。
    5. **统一动作**: 无论模式 A 或 B，都发送 `IM_PUSH` 消息到 RocketMQ（用于实时在线推送）。

#### 3.3 事务消息发送 (Transactional Messaging)
这是保证 **“数据库插入成功，MQ 消息一定发出”** 的关键（2PC）。

+ **交互流程**:
    1. **Half Message**: `im-service` 向 RocketMQ 发送“半消息”（消费者不可见）。
    2. **Local Transaction**:
        * 执行 SQL: `INSERT INTO chat_messages ...`
        * 执行 SQL: `INSERT INTO chat_inbox ...` (如果是写扩散)
    3. **Commit/Rollback**:
        * DB 成功 -> 发送 Commit -> 消息对 `im-gateway` 和 `sync-worker` 可见。
        * DB 失败 -> 发送 Rollback -> 删除半消息。
    4. **回查 (Check)**: 如果 Commit 丢失，RocketMQ 回调检查 `chat_messages` 表是否存在该 `message_id`。

#### 3.4 消息同步 (Sync/Pull)
客户端通过“信令同步”机制拉取消息。

+ **接口**: `POST /api/im/sync`
+ **参数**: `cursor` (客户端当前最大的 seq_id)
+ **返回**: `List<Message>`, `new_cursor`, `has_more`
+ **实现逻辑**:

```java
if (isSmallGroup(channelId)) {
    // 查 Inbox (极快)
    return inboxRepo.findByUserIdAndSeqGreaterThan(userId, cursor);
} else {
    // 查 Timeline (利用分区剪枝)
    // 必须带上 created_at 范围 (通常由 cursor 推算或限制最近7天)
    return timelineRepo.findByChannelIdAndSeqGreaterThan(channelId, cursor);
}
```

---

### 4. 关键代码实现 (Java)
#### 4.1 RocketMQ 事务监听器
```java
@Component
@RocketMQTransactionListener
public class MsgTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired
    private ChatMessageMapper msgMapper;
    @Autowired
    private ChatInboxMapper inboxMapper;

    /**
     * 执行本地事务 (写库)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            MsgPayload payload = (MsgPayload) arg;
            
            // 1. 插入 Timeline (所有模式必做)
            msgMapper.insert(payload.toTimelineEntity());
            
            // 2. 插入 Inbox (如果是写扩散)
            if (payload.isWriteDiffusion()) {
                 List<InboxEntity> inboxList = createInboxEntries(payload);
                 inboxMapper.batchInsert(inboxList);
            }
            
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            log.error("Msg insert failed", e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 事务回查 (当 Commit 丢失时触发)
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String msgId = msg.getHeaders().get("biz_msg_id").toString();
        // 查询数据库是否存在该消息
        int count = msgMapper.countByMsgId(Long.valueOf(msgId));
        return count > 0 ? RocketMQLocalTransactionState.COMMIT : RocketMQLocalTransactionState.ROLLBACK;
    }
}
```

#### 4.2 消息发送服务 (MsgService)
```java
@Service
public class MsgService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private GroupService groupService;

    public void sendMessage(Long senderId, SendReq req) {
        // 1. 权限校验 (是否在群里, 是否被禁言)
        checkPermission(senderId, req.getChannelId());

        // 2. 生成 SeqID (Redis 原子递增)
        Long seqId = redisTemplate.opsForValue().increment("channel:seq:" + req.getChannelId());
        
        // 3. 生成 MessageID (雪花算法)
        Long msgId = SnowflakeIdWorker.nextId();

        // 4. 构建 Payload
        MsgPayload payload = new MsgPayload();
        payload.setMsgId(msgId);
        payload.setSeqId(seqId);
        // ... 设置内容 ...

        // 5. 决策: 写扩散还是读扩散?
        int memberCount = groupService.getMemberCount(req.getChannelId());
        payload.setWriteDiffusion(memberCount < 500); // 阈值

        // 6. 发送半消息 (触发事务监听器)
        // message_id 放入 header 用于回查
        Message<String> mqMsg = MessageBuilder.withPayload(JSON.toJSONString(payload))
                                              .setHeader("biz_msg_id", msgId)
                                              .build();
                                              
        rocketMQTemplate.sendMessageInTransaction("IM_PUSH_TOPIC", mqMsg, payload);
    }
}
```

---

### 5. 性能与一致性保障
#### 5.1 数据库分区策略 (Partitioning)
PostgreSQL 的 `chat_messages` 表必须按月分区。

+ **配置**: 使用 `pg_partman` 插件。
+ **查询优化**: 在查询历史消息时，尽量带上 `created_at` 范围。
    - 例如：客户端拉取时，如果 cursor 是很久以前的，后端先计算该 cursor 大概的时间范围，缩小 SQL 扫描的分区。

#### 5.2 读写分离与缓存
+ **用户信息/群信息**: 使用 Redis 缓存 (`@Cacheable`)，TTL 1小时。
+ **消息体**: 通常**不缓存**消息体到 Redis。
    - 原因：消息体数据量大，且具有时间局部性，OS Page Cache (Postgres 内存缓冲) 已经做得很好。
    - 例外：首页“会话列表”的 `last_msg`，这个缓存在 `channels` 表的 JSON 字段中，或者 Redis 的 `session:list:{uid}` 中。

#### 5.3 消息未读数 (Unread Count)
+ **方案**: **Redis Hash + Write Behind**。
    - 存储: `im:unread:{uid} -> { channel_id: count }`。
    - 增加: 收到消息时，`HINCRBY`。
    - 清零: 进入会话时，`HSET 0`，并异步更新 DB `channel_members.last_read_seq`。
+ **Total 未读**: 维护一个总数计数器，或者前端将 Hash 所有值相加。

---

### 6. 数据交互接口定义
#### 6.1 MQ 消息体 (Push Event)
这是 `im-service` 发给 `im-gateway` 的广播消息。

```json
{
  "topic": "IM_PUSH_TOPIC",
  "tags": "MSG_NEW",
  "key": "msg_123456789",
  "body": {
    "header": {
       "cmd": 3,
       "seq": 105,
       "timestamp": 1710000000
    },
    "payload": {
       "channelId": 888,
       "senderId": 1001,
       "content": "{\"text\": \"Hello World\"}",
       "mentions": [1002, 1003]
    },
    "receivers": [1002, 1003, ...] // 如果是小群，这里列出 receiverId；大群则为空
  }
}
```

### 7. 总结
`im-service` 的设计核心是 **“快写入”** 和 **“稳投递”**。  
通过 **RocketMQ 事务消息**，我们解决了分布式系统中最棘手的“DB和MQ一致性”问题；通过 **Redis SeqID**，我们解决了消息乱序问题；通过 **Postgres 分区**，我们解决了数据爆炸问题。这是一个经得起高并发考验的后端设计。

