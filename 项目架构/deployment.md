# 🚀 企业级协作平台 - 部署运维手册

本文档提供完整的生产环境部署方案，包括基础设施准备、服务部署、监控告警配置等。

---

## 📋 目录
1. [基础设施需求](#基础设施需求)
2. [中间件部署](#中间件部署)
3. [微服务部署](#微服务部署)
4. [监控与日志](#监控与日志)
5. [备份与容灾](#备份与容灾)
6. [性能调优](#性能调优)

---

## 🖥️ 基础设施需求

### 生产环境推荐配置

| 组件 | 节点数 | CPU | 内存 | 磁盘 | 用途 |
|------|--------|-----|------|------|------|
| **PostgreSQL 主库** | 1 | 16核 | 64GB | 2TB SSD (RAID10) | 业务数据 |
| **PostgreSQL 从库** | 2 | 16核 | 64GB | 2TB SSD | 读写分离 |
| **Redis Cluster** | 6 | 8核 | 32GB | 500GB SSD | 缓存/SeqID |
| **RocketMQ Broker** | 3 | 8核 | 16GB | 1TB SSD | 消息队列 |
| **Elasticsearch** | 3 | 8核 | 32GB | 1TB SSD | 搜索索引 |
| **Neo4j** | 1 | 8核 | 32GB | 500GB SSD | 权限图谱 |
| **MinIO** | 4 | 8核 | 16GB | 10TB HDD (纠删码) | 对象存储 |
| **业务服务(Java)** | N | 4核 | 8GB | 100GB | 弹性伸缩 |
| **IM Gateway(Netty)** | 3 | 8核 | 16GB | 100GB | 长连接 |
| **Collab(Node.js)** | 3 | 4核 | 8GB | 100GB | 文档协同 |
| **Nginx** | 2 | 4核 | 8GB | 100GB | 负载均衡 |

### 网络规划
```
┌─────────────────────────────────────────────┐
│  外网 (Internet)                             │
└──────────────┬──────────────────────────────┘
               │
         ┌─────▼─────┐
         │  Nginx LB │ (公网IP)
         │ (Keepalived VIP) │
         └─────┬─────┘
               │
┌──────────────┴─────────────────────────────┐
│          DMZ 区 (10.0.1.0/24)               │
│  ┌─────────┐  ┌─────────┐  ┌──────────┐   │
│  │ Gateway │  │IM Gateway│  │ Collab   │   │
│  │ Service │  │  (Netty) │  │ (Node.js)│   │
│  └─────────┘  └─────────┘  └──────────┘   │
└──────────────┬─────────────────────────────┘
               │
┌──────────────▼─────────────────────────────┐
│       业务服务区 (10.0.2.0/24)              │
│  Auth │ User │ IM │ File │ Search │ ...    │
└──────────────┬─────────────────────────────┘
               │
┌──────────────▼─────────────────────────────┐
│         数据层 (10.0.3.0/24)                │
│  PG │ Redis │ MQ │ ES │ Neo4j │ MinIO     │
└────────────────────────────────────────────┘
```

---

## 🔧 中间件部署

### 1. PostgreSQL 15 主从部署

#### 主库配置 (`postgresql.conf`)
```ini
# 连接与认证
max_connections = 500
shared_buffers = 16GB
effective_cache_size = 48GB

# WAL 配置 (流复制)
wal_level = replica
max_wal_senders = 10
wal_keep_size = 10GB
synchronous_commit = on

# 分区表优化
enable_partition_pruning = on
constraint_exclusion = partition

# 监控
shared_preload_libraries = 'pg_stat_statements'
track_activity_query_size = 4096
```

#### 从库配置 (`recovery.conf`)
```ini
standby_mode = on
primary_conninfo = 'host=10.0.3.10 port=5432 user=replicator password=xxx'
restore_command = 'cp /var/lib/pgsql/archive/%f %p'
```

#### 分区自动维护脚本
```bash
#!/bin/bash
# /usr/local/bin/pg_partition_create.sh
# 每月1号自动执行

PGPASSWORD=xxx psql -U postgres -d collab_db -c "
SELECT create_partition_if_not_exists('chat_messages');
SELECT create_partition_if_not_exists('chat_inbox');
"
```

---

### 2. Redis Cluster 部署

#### 集群搭建
```bash
# 创建6个节点 (3主3从)
for port in 7000 7001 7002 7003 7004 7005; do
  mkdir -p /data/redis-cluster/${port}
  cat > /data/redis-cluster/${port}/redis.conf <<EOF
port ${port}
cluster-enabled yes
cluster-config-file nodes-${port}.conf
cluster-node-timeout 5000
appendonly yes
appendfsync everysec
maxmemory 28gb
maxmemory-policy noeviction
EOF
done

# 启动集群
redis-cli --cluster create \
  10.0.3.20:7000 10.0.3.21:7001 10.0.3.22:7002 \
  10.0.3.23:7003 10.0.3.24:7004 10.0.3.25:7005 \
  --cluster-replicas 1
```

#### Redis 监控命令
```bash
# 查看集群状态
redis-cli -c -p 7000 cluster info
redis-cli -c -p 7000 cluster nodes

# 监控关键指标
redis-cli -p 7000 INFO stats | grep keyspace_hits
redis-cli -p 7000 INFO memory | grep used_memory_human
```

---

### 3. RocketMQ 集群部署

#### Broker 配置 (`broker-a.conf`)
```properties
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
namesrvAddr = 10.0.3.30:9876;10.0.3.31:9876
storePathRootDir = /data/rocketmq/store-a
storePathCommitLog = /data/rocketmq/store-a/commitlog

# 高可用配置
brokerRole = ASYNC_MASTER
flushDiskType = ASYNC_FLUSH

# 性能调优
sendMessageThreadPoolNums = 16
useReentrantLockWhenPutMessage = true
transientStorePoolEnable = true

# 事务消息
transactionCheckMax = 15
transactionTimeOut = 6000
```

#### Docker Compose 快速部署
```yaml
version: '3.8'
services:
  namesrv:
    image: apache/rocketmq:5.1.4
    container_name: rmqnamesrv
    ports:
      - 9876:9876
    command: sh mqnamesrv

  broker:
    image: apache/rocketmq:5.1.4
    container_name: rmqbroker
    ports:
      - 10909:10909
      - 10911:10911
    environment:
      - NAMESRV_ADDR=rmqnamesrv:9876
    command: sh mqbroker -c /opt/rocketmq/conf/broker.conf
    volumes:
      - ./broker.conf:/opt/rocketmq/conf/broker.conf
      - /data/rocketmq:/data/rocketmq
```

---

### 4. Elasticsearch 集群部署

#### 节点配置 (`elasticsearch.yml`)
```yaml
cluster.name: collab-es-cluster
node.name: es-node-1
node.roles: [ master, data, ingest ]

network.host: 10.0.3.40
http.port: 9200

discovery.seed_hosts: ["10.0.3.40", "10.0.3.41", "10.0.3.42"]
cluster.initial_master_nodes: ["es-node-1", "es-node-2", "es-node-3"]

# 内存优化
bootstrap.memory_lock: true

# 性能调优
indices.memory.index_buffer_size: 30%
thread_pool.write.queue_size: 1000
```

#### IK 分词器安装
```bash
cd /usr/share/elasticsearch/plugins
mkdir ik
cd ik
wget https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.0/elasticsearch-analysis-ik-8.11.0.zip
unzip elasticsearch-analysis-ik-8.11.0.zip
rm elasticsearch-analysis-ik-8.11.0.zip
```

---

### 5. MinIO 分布式部署

#### 纠删码模式 (4节点)
```bash
# 启动命令
export MINIO_ROOT_USER=admin
export MINIO_ROOT_PASSWORD=your_secure_password

minio server \
  http://10.0.3.50/data{1...4} \
  http://10.0.3.51/data{1...4} \
  http://10.0.3.52/data{1...4} \
  http://10.0.3.53/data{1...4} \
  --console-address ":9001"
```

#### Bucket 策略配置
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::avatars/*"],
      "Condition": {
        "StringLike": {
          "aws:Referer": ["https://yourcompany.com/*"]
        }
      }
    }
  ]
}
```

---

## 🐳 微服务部署 (Kubernetes)

### Namespace 规划
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: collab-prod
---
apiVersion: v1
kind: Namespace
metadata:
  name: collab-middleware
```

### Gateway Service 部署示例
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-service
  namespace: collab-prod
spec:
  replicas: 3
  selector:
    matchLabels:
      app: gateway-service
  template:
    metadata:
      labels:
        app: gateway-service
    spec:
      containers:
      - name: gateway
        image: registry.yourcompany.com/gateway-service:v1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: NACOS_SERVER
          value: "nacos.collab-middleware.svc.cluster.local:8848"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: gateway-service
  namespace: collab-prod
spec:
  selector:
    app: gateway-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: gateway-ingress
  namespace: collab-prod
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: api.yourcompany.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: gateway-service
            port:
              number: 80
```

### IM Gateway (Netty) 部署
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: im-gateway
  namespace: collab-prod
spec:
  serviceName: im-gateway-headless
  replicas: 3
  selector:
    matchLabels:
      app: im-gateway
  template:
    metadata:
      labels:
        app: im-gateway
    spec:
      containers:
      - name: im-gateway
        image: registry.yourcompany.com/im-gateway:v1.0.0
        ports:
        - containerPort: 9000  # WebSocket端口
        env:
        - name: SERVER_PORT
          value: "9000"
        - name: POD_IP
          valueFrom:
            fieldRef:
              fieldPath: status.podIP
        resources:
          requests:
            memory: "4Gi"
            cpu: "2000m"
          limits:
            memory: "8Gi"
            cpu: "4000m"
        # 调整系统参数以支持C10K
        securityContext:
          capabilities:
            add:
            - NET_ADMIN
---
apiVersion: v1
kind: Service
metadata:
  name: im-gateway-lb
  namespace: collab-prod
spec:
  type: LoadBalancer
  selector:
    app: im-gateway
  ports:
  - protocol: TCP
    port: 9000
    targetPort: 9000
  sessionAffinity: ClientIP  # 保持连接稳定性
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800  # 3小时
```

### Collab Service (Node.js) 部署
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: collab-service
  namespace: collab-prod
spec:
  replicas: 3
  selector:
    matchLabels:
      app: collab-service
  template:
    metadata:
      labels:
        app: collab-service
    spec:
      containers:
      - name: collab
        image: registry.yourcompany.com/collab-service:v1.0.0
        ports:
        - containerPort: 3000
        env:
        - name: NODE_ENV
          value: "production"
        - name: REDIS_URL
          value: "redis://redis-cluster.collab-middleware.svc.cluster.local:7000"
        - name: JWT_PUBLIC_KEY
          valueFrom:
            secretKeyRef:
              name: jwt-keys
              key: public.pem
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

---

## 📊 监控与日志

### Prometheus 监控指标

#### JVM 监控 (Spring Boot Actuator)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 关键指标告警规则
```yaml
# prometheus-rules.yml
groups:
- name: collab_alerts
  rules:
  # API 响应时间告警
  - alert: HighAPILatency
    expr: histogram_quantile(0.99, http_request_duration_seconds_bucket{job="gateway-service"}) > 1
    for: 5m
    annotations:
      summary: "API P99延迟超过1秒"
  
  # 消息队列积压
  - alert: RocketMQBacklog
    expr: rocketmq_consumer_lag > 10000
    for: 10m
    annotations:
      summary: "消息队列积压超过1万条"
  
  # Redis连接数告警
  - alert: RedisHighConnections
    expr: redis_connected_clients > 5000
    for: 5m
    annotations:
      summary: "Redis连接数过高"
```

### Grafana Dashboard 模板

#### 系统总览面板
- **QPS**: `rate(http_requests_total[1m])`
- **延迟**: `histogram_quantile(0.99, http_request_duration_seconds_bucket)`
- **错误率**: `rate(http_requests_total{status=~"5.."}[1m])`
- **在线用户数**: `im_gateway_active_connections`

#### 数据库监控
- **PG TPS**: `pg_stat_database_xact_commit + pg_stat_database_xact_rollback`
- **慢查询**: `pg_slow_queries_count`
- **分区表大小**: `pg_table_size('chat_messages_2025_11')`

---

## 🔐 安全加固

### SSL/TLS 配置 (Nginx)
```nginx
server {
    listen 443 ssl http2;
    server_name api.yourcompany.com;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000" always;
    
    location / {
        proxy_pass http://gateway-service.collab-prod.svc.cluster.local;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 数据库加密
```sql
-- 敏感字段加密 (使用pgcrypto扩展)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 加密存储
INSERT INTO user_auths (credential) VALUES (
  crypt('user_password', gen_salt('bf', 12))
);

-- 验证
SELECT * FROM user_auths WHERE 
  credential = crypt('user_password', credential);
```

---

## 💾 备份与容灾

### 数据库备份策略

#### 全量备份 (每日凌晨)
```bash
#!/bin/bash
# /usr/local/bin/pg_backup.sh
DATE=$(date +%Y%m%d)
BACKUP_DIR="/backup/postgresql"

pg_dump -U postgres -Fc collab_db > ${BACKUP_DIR}/collab_db_${DATE}.dump

# 上传到对象存储
aws s3 cp ${BACKUP_DIR}/collab_db_${DATE}.dump s3://backup-bucket/postgresql/

# 保留最近7天
find ${BACKUP_DIR} -name "*.dump" -mtime +7 -delete
```

#### WAL 归档 (持续)
```ini
# postgresql.conf
archive_mode = on
archive_command = 'aws s3 cp %p s3://backup-bucket/wal/%f'
```

### Redis 持久化
```ini
# AOF + RDB 混合持久化
appendonly yes
appendfsync everysec
save 900 1
save 300 10
save 60 10000
aof-use-rdb-preamble yes
```

### 异地容灾方案
- **主站**: 上海机房
- **备站**: 深圳机房
- **同步方式**: PostgreSQL流复制 + MinIO跨区域复制
- **RTO**: < 1小时
- **RPO**: < 5分钟

---

## ⚡ 性能调优

### Linux 内核参数调优
```bash
# /etc/sysctl.conf
# 文件句柄数
fs.file-max = 1000000

# 网络优化
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 30

# 调大端口范围
net.ipv4.ip_local_port_range = 10000 65000

# 应用生效
sysctl -p
```

### JVM 参数优化
```bash
JAVA_OPTS="
  -Xms4g -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/var/log/java_heapdump.hprof
  -Dspring.profiles.active=prod
"
```

### PostgreSQL 连接池配置 (HikariCP)
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 📞 故障处理手册

### 常见故障与排查

#### 1. 消息发送失败
**现象**: 客户端报错"发送失败"  
**排查**:
```bash
# 1. 检查RocketMQ Broker状态
sh mqadmin clusterList -n 10.0.3.30:9876

# 2. 检查事务消息回查
tail -f /data/rocketmq/logs/broker.log | grep "checkTransaction"

# 3. 检查PostgreSQL连接
psql -U postgres -c "SELECT count(*) FROM pg_stat_activity WHERE state='active';"
```

#### 2. WebSocket连接频繁断开
**排查**:
```bash
# 检查Nginx超时配置
grep -r "proxy_read_timeout" /etc/nginx/

# 检查IM Gateway日志
kubectl logs -f im-gateway-0 -n collab-prod | grep "IdleStateEvent"
```

#### 3. 文档协同卡顿
**排查**:
```bash
# 检查Redis Stream积压
redis-cli XLEN yjs:stream

# 检查Node.js内存
kubectl top pod -l app=collab-service -n collab-prod
```

---

## 🎓 运维团队分工

| 角色 | 职责 | 值班安排 |
|------|------|----------|
| **SRE** | 监控告警、故障处理、性能调优 | 7x24轮班 |
| **DBA** | 数据库备份、慢查询优化、分区管理 | 工作日 |
| **开发** | 代码部署、日志分析、Bug修复 | On-call |

---

## 📚 参考资料

- [PostgreSQL官方文档 - 分区表](https://www.postgresql.org/docs/15/ddl-partitioning.html)
- [RocketMQ运维指南](https://rocketmq.apache.org/docs/deploymentOperations/01deploy)
- [Kubernetes生产最佳实践](https://kubernetes.io/docs/setup/production-environment/)
- [Elasticsearch集群优化](https://www.elastic.co/guide/en/elasticsearch/reference/current/tune-for-indexing-speed.html)

---

<div align="center">
  <strong>🔧 持续优化，稳定为王 🔧</strong>
</div>
