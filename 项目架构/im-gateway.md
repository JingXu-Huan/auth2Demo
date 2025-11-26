与处理 HTTP 短请求的 `gateway-service` 不同，`im-gateway` 是一个**有状态的、基于 Netty 的高性能接入层**。它的核心目标是解决 **C10K/C100K 问题**（即单机同时维持数万甚至数十万 TCP/WebSocket 长连接），并负责消息的实时下发。

---

### 1. 核心定位与技术选型
+ **核心职责**：
    - **连接维持**：维护客户端与服务端的长连接（WebSocket/TCP）。
    - **协议解析**：处理 Protobuf 二进制协议的编解码。
    - **会话管理**：绑定 `UserID <-> Netty Channel` 的关系。
    - **消息下发**：消费 RocketMQ 消息，推送到指定用户的 Channel。
+ **技术栈**：
    - **网络框架**：**Netty 4.1** (Java 领域高性能网络标准)。
    - **通信协议**：**WebSocket** (Web/小程序/移动端通用) + **Protobuf** (Google 高效序列化)。
    - **集群状态**：**Redis** (存储用户在线状态：`uid -> gateway_ip`)。
    - **消息总线**：**RocketMQ** (广播模式消费下行消息)。

---

### 2. 架构拓扑设计
`im-gateway` 通常不经过 Spring Cloud Gateway，而是直接由 **L4 负载均衡 (LVS/Nginx TCP Stream)** 暴露给客户端，以减少链路损耗。

```graphviz
graph TD
    Client[客户端 (Web/App)] -- WebSocket/TCP --> Nginx[Nginx (L4/L7 LoadBalancer)]
    Nginx -- IP-Hash/User-ID 路由 --> GW_Target[IM-Gateway 节点]
    
    subgraph IM-Gateway 内部
        NettyServer[Netty Server\n(Maintain Connection)]
        SessionMap[Local Session Map\n(UserID <-> Channel)]
    end
    
    GW_Target <--> Redis[Redis Cluster\n(User:UserID <-> GW_IP)]
    
    Backend[IM-Service\n(消息发送者)] -- 1. 查询 Redis 获取目标 GW IP --> Redis
    Backend -- 2. 直接发送(RPC/内部MQ/HTTP) --> GW_Target
    
    style Backend fill:#f9f,stroke:#333
    style GW_Target fill:#ddf,stroke:#333
    style Nginx fill:#fdd,stroke:#333
```

---

### 3. 核心模块详细设计
#### 3.1 网络接入与协议栈 (Pipeline Design)
Netty 的强大之处在于 ChannelPipeline。我们需要精心设计 Handler 链。

+ **入站流程 (Inbound)**：
    1. `IdleStateHandler`: **空闲检测**（如 180s 无读写则断开，防僵尸连接）。
    2. `HttpServerCodec` & `HttpObjectAggregator`: 处理 WebSocket 握手前的 HTTP 升级请求。
    3. `WebSocketServerProtocolHandler`: 处理 WS 握手、Ping/Pong、分帧。
    4. `ProtobufDecoder`: 将 WS Frame 中的二进制数据解码为 Java 对象 (`IMPacket`)。
    5. `AuthHandler`: **鉴权处理器**（握手成功后的第一道关卡）。
    6. `BusinessHandler`: 业务逻辑分发（心跳响应、上行消息转发）。
+ **出站流程 (Outbound)**：
    1. `ProtobufEncoder`: 将 Java 对象编码为二进制。
    2. `WebSocketFrameEncoder`: 包装成 WS Binary Frame 发送。

#### 3.2 会话管理 (Session Management)
这是 `im-gateway` 的心脏。我们需要维护两份状态。

1. **本地会话 (Local Session)**:
    - **结构**: `ConcurrentHashMap<Long, Channel> userChannelMap`。
    - **作用**: 当收到发给 UserID=1001 的消息时，快速找到对应的 Netty Channel 进行 `writeAndFlush`。
2. **全局会话 (Global Session / Registry)**:
    - **存储**: Redis String。Key: `im:location:{userId}`, Value: `{gateway_ip}:{port}`。
    - **作用**: 让后端服务知道用户连接在哪个 Gateway 节点上（用于踢人或定向路由）。
    - **生命周期**:
        * **连接建立**: 写入 Redis。
        * **连接断开**: 删除 Redis Key。
        * **应用重启**: 清空该 IP 下的所有 Key (利用 Redis Set 辅助索引)。

#### 3.3 鉴权握手流程
WebSocket 连接建立初期是不安全的，必须强制鉴权。

1. **连接**: 客户端发起 `ws://gateway-ip/ws?token=JWT_TOKEN`。
2. **握手拦截**: 在 `AuthHandler` 中提取 URL 参数中的 Token。
3. **校验**:
    - 解析 JWT，验证签名。
    - **风控检查**: 查 Redis `user_punishments` 和 `user_devices` (踢人逻辑)。
4. **绑定**:
    - 校验通过：将 UserID 保存到 Channel 的 `AttributeKey` 中。
    - 加入 `Local Session Map`。
    - 更新 Redis `Global Session`。
    - 校验失败：直接 `ctx.close()`。

#### 3.4 消息下行 (Push Model)
如何将一条消息推送到用户手机上？这里采用 **MQ 广播模式**，这是最简单且扩展性最好的方式。

+ **生产者**: `im-service` 处理完业务逻辑后，发送消息到 RocketMQ Topic `IM_PUSH_TOPIC`。
+ **消费者**: 所有 `im-gateway` 节点都订阅该 Topic，模式为 **BROADCASTING (广播模式)**。
+ **处理逻辑**:

```java
// 伪代码
onMessage(PushMessage msg) {
    Long targetUserId = msg.getReceiverId();
    // 1. 检查该用户是否连接在当前节点
    Channel channel = SessionManager.getChannel(targetUserId);
    
    if (channel != null && channel.isActive()) {
        // 2. 如果在，直接推送
        channel.writeAndFlush(new IMPacket(msg));
    } else {
        // 3. 如果不在，直接丢弃（因为是广播，其他节点会处理）
        // 这种方式虽然有一定带宽浪费，但避免了复杂的路由寻址，且Netty判断Map开销极小
    }
}
```

#### 3.5 心跳保活 (Heartbeat)
移动端网络极不稳定，且会有 NAT 超时问题。

+ **策略**: **智能心跳**。
    - 客户端每 30s 发送一次 `PING` 包。
    - 服务端回复 `PONG` 包。
    - **读空闲超时**: 服务端 `IdleStateHandler` 设置为 90s。如果 90s 内没收到任何数据（包括 Ping），认为连接假死，强制关闭。

---

### 4. 核心协议定义 (Protobuf)
不要使用 JSON 作为长连接协议，带宽和解析成本太高。

```protobuf
syntax = "proto3";

package com.company.im.protocol;

// 顶层数据包
message IMPacket {
    Header header = 1;
    bytes body = 2; // 根据 command 解析为具体的 Message 对象
}

message Header {
    int32 command = 1; // 指令: 1=AUTH, 2=HEARTBEAT, 3=MSG_PUSH, 4=MSG_ACK
    int32 version = 2;
    string client_id = 3; // 设备ID
    int64 seq = 4;        // 请求序号
}

// 消息推送体 (Command = 3)
message PushMsgBody {
    int64 sender_id = 1;
    int64 receiver_id = 2;
    int64 group_id = 3;   // 0 表示单聊
    int32 type = 4;       // 消息类型
    string content = 5;   // JSON 字符串
    int64 msg_id = 6;
}
```

---

### 5. 关键代码实现 (Java)
#### 5.1 Netty Server Bootstrap
```java
@Component
public class NettyServer {

    @PostConstruct
    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            // 操作系统参数调优
            .option(ChannelOption.SO_BACKLOG, 1024) // 握手队列长度
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true) // 禁用 Nagle 算法，降低延迟
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    // 3分钟无读写则触发 UserEvent
                    p.addLast(new IdleStateHandler(180, 0, 0));
                    p.addLast(new HttpServerCodec());
                    p.addLast(new HttpObjectAggregator(65536));
                    p.addLast(new WebSocketServerProtocolHandler("/ws"));
                    p.addLast(new ProtobufVarint32FrameDecoder()); // 处理半包/粘包
                    p.addLast(new ProtobufDecoder(IMPacket.getDefaultInstance()));
                    p.addLast(authHandler); // 鉴权
                    p.addLast(imBusinessHandler); // 业务
                }
            });
        
        bootstrap.bind(port).sync();
    }
}
```

#### 5.2 鉴权处理器 (`AuthHandler`)
```java
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<IMPacket> {

    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMPacket packet) {
        if (packet.getHeader().getCommand() == Command.AUTH) {
            String token = packet.getBody().toStringUtf8();
            // 1. 校验 Token
            if (jwtUtils.verify(token)) {
                Long userId = jwtUtils.getUserId(token);
                
                // 2. 绑定 Session
                sessionManager.addSession(userId, ctx.channel());
                
                // 3. 移除 AuthHandler (鉴权通过后就不需要它了，提升性能)
                ctx.pipeline().remove(this);
            } else {
                ctx.close();
            }
        } else {
            // 未鉴权发送了其他包，直接关闭
            ctx.close();
        }
    }
}
```

### 6. 性能优化 (C100K Tuning)
要让单个 `im-gateway` 节点支撑 10万+ 连接，必须进行 OS 级别调优：

1. **文件句柄数 (File Descriptors)**:
    - Linux 默认是 1024。必须修改 `/etc/security/limits.conf`，设置 `* hard nofile 1000000`。
2. **堆内存管理**:
    - 每一个连接大约占用 3-5KB 内存（不含业务对象）。
    - 使用 Netty 的 `PooledByteBufAllocator` 减少 GC。
    - 建议 JVM 堆内存设置在 4G-8G，不宜过大，配合 G1 或 ZGC 垃圾回收器。
3. **连接风暴处理**:
    - 当服务重启时，数万客户端会同时重连。
    - **策略**: 客户端实现“随机退避算法”（Random Backoff），不要立即重连，而是随机等待 1-10秒。

### 总结
这个 `im-gateway` 模块是独立的、轻量级的，它像一个**高效的路由器**，只负责维持管道和分发数据，不处理复杂的业务逻辑（如存库、发推送），从而保证了极高的吞吐量和稳定性。这是构建企业级 IM 的基石。

