-- ========================================================================
-- 消息队列与事件系统 (Message Queue & Event Database)
-- 支持RocketMQ事务消息、事件溯源、Webhook等
-- ========================================================================

CREATE DATABASE event_db;
\c event_db;

-- 7.1 本地消息表 (事务消息 - 核心表)
CREATE TABLE local_messages (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(100) NOT NULL UNIQUE, -- 消息唯一ID
    
    -- 消息内容
    topic VARCHAR(100) NOT NULL,
    tag VARCHAR(50),
    keys VARCHAR(200), -- 消息键（用于查询）
    payload TEXT NOT NULL, -- 消息体（JSON）
    
    -- 事务相关
    business_key VARCHAR(200), -- 业务唯一键
    business_type VARCHAR(50), -- 业务类型
    business_id VARCHAR(100), -- 业务ID
    
    -- 分片键（用于顺序消息）
    sharding_key VARCHAR(100),
    
    -- 投递状态
    status SMALLINT DEFAULT 0, -- 0:待发送, 1:已发送, 2:已确认, 3:发送失败, 4:已取消
    
    -- 重试机制
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 3,
    next_retry_at TIMESTAMPTZ,
    
    -- 错误信息
    error_code VARCHAR(50),
    error_message TEXT,
    
    -- 时间戳
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ
);

CREATE INDEX idx_local_messages_status ON local_messages(status, next_retry_at) 
    WHERE status IN (0, 3);
CREATE INDEX idx_local_messages_business ON local_messages(business_key);
CREATE INDEX idx_local_messages_created ON local_messages(created_at);

-- 7.2 事件日志表 (Event Sourcing - 分区表)
CREATE TABLE event_logs (
    id BIGSERIAL,
    event_id VARCHAR(100) NOT NULL,
    
    -- 事件信息
    event_type VARCHAR(100) NOT NULL, -- 事件类型
    event_version VARCHAR(10) DEFAULT '1.0', -- 事件版本
    
    -- 事件源
    aggregate_type VARCHAR(50), -- 聚合根类型
    aggregate_id VARCHAR(100), -- 聚合根ID
    sequence_number BIGINT, -- 事件序号
    
    -- 事件数据
    payload JSONB NOT NULL,
    metadata JSONB, -- 元数据（追踪信息等）
    
    -- 执行信息
    user_id BIGINT,
    tenant_id BIGINT,
    ip_address INET,
    user_agent TEXT,
    trace_id VARCHAR(100), -- 分布式追踪ID
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (created_at, id)
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE event_logs_2024_11 PARTITION OF event_logs
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_event_logs_event_id ON event_logs(event_id);
CREATE INDEX idx_event_logs_type ON event_logs(event_type);
CREATE INDEX idx_event_logs_aggregate ON event_logs(aggregate_type, aggregate_id);
CREATE INDEX idx_event_logs_user ON event_logs(user_id);

-- 7.3 事件快照表（加速事件溯源查询）
CREATE TABLE event_snapshots (
    id BIGSERIAL PRIMARY KEY,
    
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    
    -- 快照信息
    sequence_number BIGINT NOT NULL, -- 快照时的事件序号
    snapshot_data JSONB NOT NULL, -- 快照数据
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(aggregate_type, aggregate_id, sequence_number)
);

CREATE INDEX idx_event_snapshots_aggregate ON event_snapshots(aggregate_type, aggregate_id);

-- 7.4 Webhook订阅表
CREATE TABLE webhook_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    
    -- 订阅者信息
    app_id VARCHAR(100) NOT NULL,
    app_name VARCHAR(100),
    
    -- 订阅配置
    event_types TEXT[] NOT NULL, -- 订阅的事件类型数组
    event_filters JSONB, -- 事件过滤条件
    
    -- 回调配置
    callback_url VARCHAR(500) NOT NULL,
    callback_method VARCHAR(10) DEFAULT 'POST',
    callback_headers JSONB, -- 自定义请求头
    
    -- 安全配置
    secret VARCHAR(100) NOT NULL, -- 签名密钥
    auth_type VARCHAR(20), -- none, basic, bearer, hmac
    auth_credentials JSONB, -- 认证凭证
    
    -- 重试配置
    retry_config JSONB DEFAULT '{"max_retry": 3, "timeout": 5, "backoff": "exponential"}',
    
    -- 状态
    active BOOLEAN DEFAULT TRUE,
    verified BOOLEAN DEFAULT FALSE, -- 是否已验证
    
    -- 限流
    rate_limit INT DEFAULT 1000, -- 每分钟请求限制
    
    -- 统计
    total_events INT DEFAULT 0,
    success_count INT DEFAULT 0,
    failure_count INT DEFAULT 0,
    last_delivery_at TIMESTAMPTZ,
    last_error TEXT,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_webhook_subs_app ON webhook_subscriptions(app_id);
CREATE INDEX idx_webhook_subs_active ON webhook_subscriptions(active) WHERE active = TRUE;
CREATE INDEX idx_webhook_subs_event_types ON webhook_subscriptions USING GIN (event_types);

-- 7.5 Webhook投递记录表
CREATE TABLE webhook_deliveries (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES webhook_subscriptions(id),
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    
    -- 请求信息
    request_url VARCHAR(500),
    request_method VARCHAR(10),
    request_headers JSONB,
    request_body TEXT,
    request_signature VARCHAR(200), -- HMAC签名
    
    -- 响应信息
    response_status INT,
    response_headers JSONB,
    response_body TEXT,
    response_time_ms INT, -- 响应时间（毫秒）
    
    -- 投递状态
    status SMALLINT, -- 1:成功, 2:失败, 3:超时
    retry_count INT DEFAULT 0,
    
    -- 错误信息
    error_type VARCHAR(50),
    error_message TEXT,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ
);

CREATE INDEX idx_webhook_deliveries_subscription ON webhook_deliveries(subscription_id);
CREATE INDEX idx_webhook_deliveries_event ON webhook_deliveries(event_id);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries(status);
CREATE INDEX idx_webhook_deliveries_retry ON webhook_deliveries(next_retry_at) 
    WHERE status = 2 AND retry_count < 3;

-- 7.6 定时任务表
CREATE TABLE scheduled_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL,
    task_type VARCHAR(50) NOT NULL, -- cron, delay, rate
    
    -- 任务配置
    cron_expression VARCHAR(100), -- Cron表达式
    delay_seconds INT, -- 延迟秒数
    rate_seconds INT, -- 执行频率
    
    -- 任务内容
    handler_class VARCHAR(200), -- 处理器类
    parameters JSONB, -- 任务参数
    
    -- 执行配置
    max_retry INT DEFAULT 3,
    timeout_seconds INT DEFAULT 300,
    
    -- 状态
    enabled BOOLEAN DEFAULT TRUE,
    
    -- 执行信息
    last_run_at TIMESTAMPTZ,
    last_run_status VARCHAR(20),
    last_run_message TEXT,
    next_run_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scheduled_tasks_enabled ON scheduled_tasks(enabled);
CREATE INDEX idx_scheduled_tasks_next_run ON scheduled_tasks(next_run_at) WHERE enabled = TRUE;

-- 7.7 任务执行历史表
CREATE TABLE task_execution_history (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES scheduled_tasks(id),
    
    -- 执行信息
    execution_id VARCHAR(100) UNIQUE NOT NULL,
    worker_id VARCHAR(50), -- 执行节点
    
    -- 执行时间
    scheduled_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    
    -- 执行结果
    status VARCHAR(20) NOT NULL, -- running, success, failed, timeout
    result JSONB,
    error_message TEXT,
    
    -- 性能指标
    execution_time_ms INT,
    memory_used_mb INT,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE task_execution_history_2024_11 PARTITION OF task_execution_history
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_task_history_task ON task_execution_history(task_id);
CREATE INDEX idx_task_history_status ON task_execution_history(status);

-- 7.8 消息消费记录表（去重）
CREATE TABLE message_consumption_records (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(100) NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    
    -- 消费信息
    consumer_id VARCHAR(100),
    consumed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- 处理结果
    status SMALLINT DEFAULT 1, -- 1:成功, 2:失败
    error_message TEXT,
    
    -- 业务处理结果
    business_result JSONB,
    
    UNIQUE(message_id, consumer_group)
);

CREATE INDEX idx_consumption_records_message ON message_consumption_records(message_id);
CREATE INDEX idx_consumption_records_group ON message_consumption_records(consumer_group);

-- 7.9 死信队列表
CREATE TABLE dead_letter_queue (
    id BIGSERIAL PRIMARY KEY,
    
    -- 原始消息信息
    original_topic VARCHAR(100),
    original_tag VARCHAR(50),
    original_message_id VARCHAR(100),
    original_payload TEXT,
    
    -- 失败信息
    consumer_group VARCHAR(100),
    failure_count INT DEFAULT 1,
    last_failure_reason TEXT,
    last_failure_at TIMESTAMPTZ,
    
    -- 处理状态
    status SMALLINT DEFAULT 0, -- 0:待处理, 1:已重试, 2:已忽略, 3:已修复
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_dead_letter_status ON dead_letter_queue(status);
CREATE INDEX idx_dead_letter_topic ON dead_letter_queue(original_topic);

-- 7.10 分布式锁表
CREATE TABLE distributed_locks (
    lock_name VARCHAR(200) PRIMARY KEY,
    lock_owner VARCHAR(100) NOT NULL,
    
    -- 锁信息
    locked_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    
    -- 锁内容
    lock_data JSONB,
    
    -- 重入支持
    reentrant_count INT DEFAULT 1
);

CREATE INDEX idx_distributed_locks_expires ON distributed_locks(expires_at);

-- 7.11 幂等性记录表
CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(200) PRIMARY KEY,
    
    -- 请求信息
    service_name VARCHAR(50),
    operation VARCHAR(100),
    request_hash VARCHAR(64), -- 请求内容hash
    
    -- 响应信息
    response_status INT,
    response_data JSONB,
    
    -- 时间管理
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours')
);

CREATE INDEX idx_idempotency_expires ON idempotency_records(expires_at);
CREATE INDEX idx_idempotency_service ON idempotency_records(service_name, operation);

-- 函数：清理过期的幂等性记录
CREATE OR REPLACE FUNCTION cleanup_expired_idempotency_records()
RETURNS INTEGER AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    DELETE FROM idempotency_records
    WHERE expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql;

-- 函数：获取分布式锁
CREATE OR REPLACE FUNCTION acquire_distributed_lock(
    p_lock_name VARCHAR,
    p_lock_owner VARCHAR,
    p_ttl_seconds INT DEFAULT 60
)
RETURNS BOOLEAN AS $$
BEGIN
    -- 尝试插入新锁
    INSERT INTO distributed_locks (lock_name, lock_owner, expires_at)
    VALUES (p_lock_name, p_lock_owner, CURRENT_TIMESTAMP + (p_ttl_seconds || ' seconds')::INTERVAL)
    ON CONFLICT (lock_name) DO UPDATE
    SET lock_owner = p_lock_owner,
        locked_at = CURRENT_TIMESTAMP,
        expires_at = CURRENT_TIMESTAMP + (p_ttl_seconds || ' seconds')::INTERVAL,
        reentrant_count = distributed_locks.reentrant_count + 1
    WHERE distributed_locks.lock_owner = p_lock_owner 
       OR distributed_locks.expires_at < CURRENT_TIMESTAMP;
    
    -- 检查是否获取成功
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;
