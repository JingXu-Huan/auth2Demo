-- ========================================================================
-- 即时通讯系统 (IM Database) - 支持十亿级消息
-- ========================================================================

CREATE DATABASE im_db;
\c im_db;

-- 2.1 会话表 (Channel)
CREATE TABLE channels (
    id BIGSERIAL PRIMARY KEY,
    channel_type SMALLINT NOT NULL, -- 1:单聊, 2:群聊, 3:系统通知, 4:频道
    
    -- 群聊专用字段
    name VARCHAR(100),
    avatar_url VARCHAR(500),
    owner_id BIGINT,
    member_count INT DEFAULT 0,
    max_members INT DEFAULT 500,
    
    -- 公告与描述
    announcement TEXT,
    description TEXT,
    
    -- 配置
    settings JSONB DEFAULT '{}', -- 群设置
    features JSONB DEFAULT '{}', -- 功能开关
    
    -- 加群设置
    join_type SMALLINT DEFAULT 1, -- 1:自由加入, 2:需审批, 3:禁止加入
    join_question TEXT, -- 入群问题
    
    status SMALLINT DEFAULT 1, -- 1:正常, 2:解散, 3:封禁
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_channels_type ON channels(channel_type);
CREATE INDEX idx_channels_owner ON channels(owner_id);
CREATE INDEX idx_channels_status ON channels(status);

-- 2.2 会话成员表
CREATE TABLE channel_members (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL REFERENCES channels(id),
    user_id BIGINT NOT NULL,
    
    -- 成员信息
    role SMALLINT DEFAULT 1, -- 1:普通成员, 2:管理员, 3:群主
    nickname VARCHAR(50), -- 群昵称
    
    -- 消息相关
    last_read_msg_id BIGINT, -- 最后已读消息
    last_read_seq BIGINT, -- 最后已读序号
    last_read_time TIMESTAMPTZ,
    unread_count INT DEFAULT 0,
    
    -- 提醒设置
    mention_count INT DEFAULT 0, -- @消息数
    
    -- 设置
    muted_until TIMESTAMPTZ, -- 免打扰
    pinned BOOLEAN DEFAULT FALSE, -- 置顶
    show_nickname BOOLEAN DEFAULT TRUE,
    
    -- 邀请信息
    invited_by BIGINT,
    
    joined_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ,
    
    UNIQUE(channel_id, user_id)
);

CREATE INDEX idx_channel_members_user ON channel_members(user_id) WHERE left_at IS NULL;
CREATE INDEX idx_channel_members_channel ON channel_members(channel_id) WHERE left_at IS NULL;

-- 2.3 消息表 (分区表 - 核心表)
CREATE TABLE messages (
    message_id BIGINT NOT NULL, -- 雪花算法ID
    channel_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    
    -- 消息顺序（每个channel独立递增）
    seq_id BIGINT NOT NULL,
    
    -- 消息内容
    msg_type SMALLINT NOT NULL, -- 1:文本, 2:图片, 3:文件, 4:语音, 5:视频, 6:位置, 7:名片, 8:红包
    content TEXT,
    
    -- 媒体信息
    media_urls TEXT[], -- 媒体URL数组
    media_info JSONB, -- 媒体详情（尺寸、时长等）
    
    -- 引用与回复
    reply_to_msg_id BIGINT,
    reply_to_user_id BIGINT,
    forward_from_msg_id BIGINT,
    forward_from_channel_id BIGINT,
    
    -- @ 提醒
    mentioned_user_ids BIGINT[],
    mention_all BOOLEAN DEFAULT FALSE,
    
    -- 状态
    status SMALLINT DEFAULT 1, -- 1:正常, 2:撤回, 3:删除, 4:审核中
    edited BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMPTZ,
    
    -- 扩展
    extra JSONB, -- 扩展信息（红包信息、投票等）
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (created_at, message_id)
) PARTITION BY RANGE (created_at);

-- 创建分区索引
CREATE INDEX idx_messages_channel_seq ON messages(channel_id, seq_id);
CREATE INDEX idx_messages_sender ON messages(sender_id);

-- 创建分区 (按月)
CREATE TABLE messages_2024_11 PARTITION OF messages
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE messages_2024_12 PARTITION OF messages
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- 对老分区使用 BRIN 索引（节省空间）
-- CREATE INDEX messages_2024_01_brin ON messages_2024_01 USING BRIN (created_at);

-- 2.4 消息收件箱表 (写扩散模式 - 用于500人以下群组)
CREATE TABLE message_inbox (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    
    msg_type SMALLINT NOT NULL,
    preview TEXT, -- 消息预览（前100字符）
    
    is_read BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_mentioned BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, message_id)
) PARTITION BY HASH (user_id);

-- 创建Hash分区（32个分区）
CREATE TABLE message_inbox_0 PARTITION OF message_inbox FOR VALUES WITH (MODULUS 32, REMAINDER 0);
CREATE TABLE message_inbox_1 PARTITION OF message_inbox FOR VALUES WITH (MODULUS 32, REMAINDER 1);
-- ... 创建剩余30个分区

CREATE INDEX idx_inbox_user_unread ON message_inbox(user_id, created_at DESC) 
    WHERE is_read = FALSE AND is_deleted = FALSE;

-- 2.5 消息已读记录表
CREATE TABLE message_reads (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(message_id, user_id)
);

CREATE INDEX idx_message_reads_channel_msg ON message_reads(channel_id, message_id);
CREATE INDEX idx_message_reads_user ON message_reads(user_id);

-- 2.6 离线消息队列表
CREATE TABLE offline_message_queue (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    
    priority SMALLINT DEFAULT 1, -- 优先级
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 3,
    next_retry_at TIMESTAMPTZ,
    
    delivered BOOLEAN DEFAULT FALSE,
    delivered_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_offline_queue_user ON offline_message_queue(user_id) 
    WHERE delivered = FALSE;
CREATE INDEX idx_offline_queue_retry ON offline_message_queue(next_retry_at) 
    WHERE delivered = FALSE AND retry_count < 3;

-- 2.7 消息撤回记录表
CREATE TABLE message_recalls (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    
    recall_by BIGINT NOT NULL, -- 撤回操作者
    recall_reason TEXT,
    
    original_content TEXT, -- 原始内容（审计用）
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_message_recalls_msg ON message_recalls(message_id);

-- 2.8 消息反应表（表情回复）
CREATE TABLE message_reactions (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    emoji VARCHAR(10) NOT NULL, -- emoji符号
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(message_id, user_id, emoji)
);

CREATE INDEX idx_message_reactions_msg ON message_reactions(message_id);

-- 2.9 频道订阅表（针对广播频道）
CREATE TABLE channel_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL REFERENCES channels(id),
    user_id BIGINT NOT NULL,
    
    -- 订阅设置
    receive_push BOOLEAN DEFAULT TRUE,
    
    subscribed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at TIMESTAMPTZ,
    
    UNIQUE(channel_id, user_id)
);

CREATE INDEX idx_channel_subscriptions_user ON channel_subscriptions(user_id) 
    WHERE unsubscribed_at IS NULL;

-- 2.10 消息搜索索引表
CREATE TABLE message_search_index (
    message_id BIGINT PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    
    -- 全文搜索
    content_tsv tsvector, -- PostgreSQL全文搜索向量
    
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_message_search_tsv ON message_search_index USING GIN (content_tsv);
CREATE INDEX idx_message_search_channel ON message_search_index(channel_id);

-- 序列号生成器（每个channel独立）
CREATE TABLE channel_sequences (
    channel_id BIGINT PRIMARY KEY,
    current_seq BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 创建序列号生成函数
CREATE OR REPLACE FUNCTION get_next_seq_id(p_channel_id BIGINT)
RETURNS BIGINT AS $$
DECLARE
    v_seq BIGINT;
BEGIN
    UPDATE channel_sequences 
    SET current_seq = current_seq + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE channel_id = p_channel_id
    RETURNING current_seq INTO v_seq;
    
    IF NOT FOUND THEN
        INSERT INTO channel_sequences (channel_id, current_seq)
        VALUES (p_channel_id, 1)
        ON CONFLICT (channel_id) DO UPDATE
        SET current_seq = channel_sequences.current_seq + 1
        RETURNING current_seq INTO v_seq;
    END IF;
    
    RETURN v_seq;
END;
$$ LANGUAGE plpgsql;
