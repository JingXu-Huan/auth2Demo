这是 `admin-service`** (企业管理后台与审计服务)** 的详细架构设计。

在企业级架构中，`admin-service` 是系统的**“上帝视角”**。它不直接承载 C 端用户的高并发流量，但对**数据一致性、操作审计、权限隔离**有着极高的要求。它是 IT 管理员、合规审计员和运营人员的工作台。

---

### 1. 核心定位与职责边界
+ **核心定位**：
    - **控制塔 (Control Tower)**：对全平台的用户、组织、内容进行管控（增删改查、封禁、解散）。
    - **审计中心 (Audit Hub)**：汇聚所有敏感操作日志，提供合规报表。
    - **配置中心 (Config Center)**：管理敏感词库、文件保留策略、登录安全策略。
+ **与其他服务的关系**：
    - 它是其他所有服务（User, Auth, IM, File）的 **Super Client**。
    - 它通常通过 **Feign/Dubbo (RPC)** 调用业务服务执行变更，通过 **Elasticsearch** 查询日志和报表。

---

### 2. 技术栈清单
+ **框架**: Spring Boot 3.x
+ **权限框架**: **Spring Security** (独立于 C 端用户的鉴权体系，用于管理员 RBAC)
+ **调用方式**: OpenFeign (RPC 调用下游服务)
+ **报表引擎**: Apache POI / EasyExcel (导出 Excel), ECharts (前端图表)
+ **存储**: PostgreSQL (管理配置), Elasticsearch (审计日志查询)

---

### 3. 架构功能模块设计
```mermaid
graph TD
    AdminUser[管理员/审计员] --> AdminWeb[管理后台前端]
    AdminWeb --> AdminService[Admin-Service]

    subgraph Governance Domain [治理域]
        AdminService -- RPC --> UserService[User-Service\n(组织架构管理)]
        AdminService -- RPC --> AuthService[Auth-Service\n(封号/踢人/重置密码)]
        AdminService -- RPC --> IMService[IM-Service\n(解散群/撤回消息)]
    end

    subgraph Compliance Domain [合规域]
        AdminService -- Query --> ES[Elasticsearch\n(审计日志/消息记录)]
        AdminService -- Read/Write --> RiskDB[(PostgreSQL: 风控策略/敏感词)]
    end

    subgraph System Domain [系统域]
        AdminService -- Config --> Nacos[Nacos Config]
        AdminService -- Metrics --> Prometheus[Prometheus]
    end
```

---

### 4. 核心子模块详细设计
#### 4.1 管理员权限体系 (Admin RBAC)
**注意**：管理员的权限体系与 C 端用户（普通员工）是完全隔离的。

+ **角色模型**：
    - **Super Admin**: 拥有所有权限，唯一能创建其他管理员的人。
    - **Auditor (审计员)**: 只读权限，查看日志，查看消息记录（需特殊授权），不可修改数据。
    - **Operator (运营)**: 管理敏感词，发布全员公告。
    - **IT Admin**: 负责组织架构调整，处理员工离职交接。
+ **实现**: 使用 Spring Security 的 `@PreAuthorize("hasRole('ADMIN_SUPER')")` 进行接口级控制。

#### 4.2 用户与组织治理 (Governance)
这是管理员最高频的操作，本质上是编排调用下游服务。

+ **功能点**:
    - **封禁/解封**: 调用 `auth-service` 的 `/api/admin/user/ban`。
    - **强制下线**: 调用 `auth-service` 的 `/api/admin/device/kick`。
    - **离职交接**:
        1. 调用 `user-service` 将用户状态设为“离职”。
        2. 调用 `doc-service` 将其个人空间文档的所有权转移给主管。
        3. 调用 `im-service` 将其退群（或保留只读）。
+ **数据一致性**: 这里的操作通常涉及多个微服务，建议采用 **Saga 模式** 或 **尽最大努力通知**。例如，离职是一个长流程，`admin-service` 发送一个 `EMPLOYEE_RESIGNED` 的 MQ 消息，各服务订阅并执行各自的清理逻辑。

#### 4.3 内容安全与风控 (Content Safety)
+ **敏感词管理**:
    - **CRUD**: 管理 `sensitive_words` 表。
    - **同步**: 变更后，通过 Redis Pub/Sub 或 MQ 广播给 `im-service` 和 `collab-service`，更新它们内存中的 AC 自动机或 DFA 过滤器。
+ **消息撤回 (Global Retract)**:
    - 管理员可在后台查看被举报的消息。
    - 点击“违规删除” -> 调用 `im-service` 将消息内容替换为“该消息因违规被管理员屏蔽”。

#### 4.4 审计日志查询 (Audit Log Search)
企业合规的核心。

+ **数据源**: `sync-worker` 将全平台的 `audit_logs` 实时写入 Elasticsearch。
+ **查询能力**:
    - **Who**: 谁操作的？（IP, OperatorID）
    - **When**: 时间范围。
    - **What**: 操作对象（如“删除了部门A”）。
    - **Diff**: 变更前后的 JSON 对比（Elasticsearch 存储 JSON 结构非常合适）。
+ **导出**: 支持导出近 1 年的日志为加密 Excel。

#### 4.5 仪表盘 (Dashboard)
展示系统健康度与活跃度。

+ **在线人数**: 读取 Redis 中 `im-gateway` 上报的计数。
+ **消息吞吐**: 读取 Prometheus/Grafana 的 API 指标，或查询 ES 的聚合结果。
+ **存储用量**: 调用 MinIO API 获取 Bucket 大小。

---

### 5. 数据库设计 (PostgreSQL)
这部分表仅供 `admin-service` 使用，与业务库物理隔离。

```sql
-- 1. 管理员账号表 (独立于 users 表)
CREATE TABLE admins (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL,      -- 'SUPER_ADMIN', 'AUDITOR', 'IT_ADMIN'
    status INT DEFAULT 1,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. 敏感词库
CREATE TABLE sensitive_words (
    id SERIAL PRIMARY KEY,
    word VARCHAR(64) NOT NULL UNIQUE,
    category VARCHAR(20),           -- 'POLITICAL', 'PORN', 'AD'
    action_type INT DEFAULT 1,      -- 1:拦截, 2:替换***, 3:报警
    created_by BIGINT NOT NULL,     -- admin_id
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. 系统全局配置 (动态配置)
CREATE TABLE sys_configs (
    config_key VARCHAR(100) PRIMARY KEY, -- 如 'upload.max_size', 'im.msg_retention_days'
    config_value VARCHAR(255),
    description VARCHAR(255),
    updated_by BIGINT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. 举报/投诉记录
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL,    -- 举报人(C端用户)
    target_type VARCHAR(20),        -- 'MSG', 'USER', 'DOC'
    target_id VARCHAR(64),          -- msg_id 等
    reason VARCHAR(50),
    status INT DEFAULT 0,           -- 0:待处理, 1:已封禁, 2:已忽略
    admin_comment TEXT,             -- 处理意见
    handled_by BIGINT,              -- 处理管理员ID
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

### 6. 核心代码实现 (Java)
#### 6.1 Feign 客户端定义 (调用下游)
```java
// 调用 Auth 服务进行封禁
@FeignClient(name = "auth-service", contextId = "AuthAdminClient")
public interface AuthFeignClient {
    @PostMapping("/api/admin/user/ban")
    Result<Void> banUser(@RequestBody BanReq req);
    
    @DeleteMapping("/api/auth/devices/{deviceId}")
    Result<Void> kickDevice(@RequestParam("userId") Long userId, 
                            @PathVariable("deviceId") String deviceId);
}

// 调用 User 服务获取组织树
@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/api/org/tree")
    Result<List<DeptNode>> getOrgTree();
}
```

#### 6.2 敏感词更新与广播
```java
@Service
public class RiskControlService {

    @Autowired
    private SensitiveWordMapper wordMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Transactional
    public void addSensitiveWord(String word, int actionType) {
        // 1. 入库
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(word);
        entity.setActionType(actionType);
        wordMapper.insert(entity);

        // 2. 广播更新 (Pub/Sub)
        // 所有 IM-Service 和 Collab-Service 实例订阅该 Channel
        // 收到消息后，重新从 DB 加载词库到内存 Trie 树
        redisTemplate.convertAndSend("SYS_CONFIG_UPDATE", "SENSITIVE_WORDS");
    }
}
```

#### 6.3 审计日志 AOP 切面 (Admin 操作留痕)
记录管理员自己的操作日志。

```java
@Aspect
@Component
public class AdminAuditAspect {

    @Autowired
    private AdminAuditLogMapper logMapper;

    @AfterReturning(pointcut = "@annotation(com.company.admin.annotation.AdminLog)", returning = "result")
    public void recordLog(JoinPoint joinPoint, Object result) {
        // 1. 获取当前登录的 Admin
        Long adminId = SecurityContextHolder.getAdminId();
        
        // 2. 解析操作内容
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AdminLog annotation = signature.getMethod().getAnnotation(AdminLog.class);
        
        // 3. 异步入库
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminId(adminId);
        log.setAction(annotation.value()); // e.g., "BAN_USER"
        log.setParams(JSON.toJSONString(joinPoint.getArgs()));
        log.setIp(IpUtils.getIpAddr());
        
        logMapper.insert(log);
    }
}
```

---

### 7. 特殊场景：查看 C 端用户聊天记录
这是最敏感的功能，必须有严格的授权流程（**Two-Man Rule / 双人复核**）。

+ **流程**：
    1. 审计员 A 申请查看用户 U 最近 3 天的聊天记录，填写理由“合规调查”。
    2. 系统生成审批单。
    3. 超级管理员 B 收到通知，审批通过。
    4. 系统生成一个临时 Token，有效期 1 小时。
    5. 审计员 A 使用该 Token 调用 `im-service` 的 `/api/admin/msgs` 接口。
    6. **ES 查询**: `im-service` 从 Elasticsearch 中检索历史消息（因 PG 分区表可能已归档，且 ES 搜索更强）。
    7. **日志**: 整个过程（申请、审批、查询、导出）全程记录审计日志。

### 总结
`admin-service` 的设计不仅是功能的堆砌，更是**管理流程的数字化**。它通过 RPC 串联起所有业务微服务，通过 MQ 和 Redis 广播配置变更，通过 ES 沉淀审计数据，是一个高内聚、低耦合的管理中台。

