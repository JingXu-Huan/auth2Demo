-- ============================================
-- 消息服务数据库 (aio_message)
-- ============================================

-- 创建数据库
CREATE DATABASE aio_message;

\c aio_message;

-- ============================================
-- 1. 消息表 (messages)
-- ============================================
CREATE TABLE messages (
    message_id VARCHAR(50) PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT,
    group_id VARCHAR(50),
    message_type VARCHAR(20) NOT NULL CHECK (message_type IN ('TEXT', 'IMAGE', 'VIDEO', 'AUDIO', 'FILE', 'LOCATION', 'CARD', 'SYSTEM')),
    content TEXT NOT NULL,
    client_msg_id VARCHAR(50) UNIQUE,
    status VARCHAR(20) DEFAULT 'SENT' CHECK (status IN ('SENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'RECALLED')),
    reply_to VARCHAR(50),
    at_user_ids BIGINT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    recalled_at TIMESTAMP,
    read_at TIMESTAMP
);

-- 索引
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_receiver ON messages(receiver_id);
CREATE INDEX idx_messages_group ON messages(group_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_messages_status ON messages(status);
CREATE INDEX idx_messages_client_msg_id ON messages(client_msg_id);

-- 注释
COMMENT ON TABLE messages IS '消息表';
COMMENT ON COLUMN messages.message_id IS '消息ID';
COMMENT ON COLUMN messages.sender_id IS '发送者ID';
COMMENT ON COLUMN messages.receiver_id IS '接收者ID（单聊）';
COMMENT ON COLUMN messages.group_id IS '群组ID（群聊）';
COMMENT ON COLUMN messages.message_type IS '消息类型';
COMMENT ON COLUMN messages.content IS '消息内容（JSON格式）';
COMMENT ON COLUMN messages.client_msg_id IS '客户端消息ID（用于去重）';
COMMENT ON COLUMN messages.status IS '消息状态';
COMMENT ON COLUMN messages.reply_to IS '回复的消息ID';
COMMENT ON COLUMN messages.at_user_ids IS '@的用户ID列表';

-- ============================================
-- 2. 消息已读状态表 (message_read_status)
-- ============================================
CREATE TABLE message_read_status (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(message_id, user_id)
);

-- 索引
CREATE INDEX idx_read_status_message ON message_read_status(message_id);
CREATE INDEX idx_read_status_user ON message_read_status(user_id);

-- 注释
COMMENT ON TABLE message_read_status IS '消息已读状态表';
COMMENT ON COLUMN message_read_status.message_id IS '消息ID';
COMMENT ON COLUMN message_read_status.user_id IS '用户ID';
COMMENT ON COLUMN message_read_status.read_at IS '阅读时间';

-- ============================================
-- 3. 会话表 (conversations)
-- ============================================
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_id BIGINT,
    group_id VARCHAR(50),
    conversation_type VARCHAR(20) CHECK (conversation_type IN ('PRIVATE', 'GROUP')),
    last_message_id VARCHAR(50),
    last_message_time TIMESTAMP,
    unread_count INT DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_muted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, target_id, group_id)
);

-- 索引
CREATE INDEX idx_conversations_user ON conversations(user_id);
CREATE INDEX idx_conversations_target ON conversations(target_id);
CREATE INDEX idx_conversations_group ON conversations(group_id);
CREATE INDEX idx_conversations_last_message_time ON conversations(last_message_time);

-- 注释
COMMENT ON TABLE conversations IS '会话表';
COMMENT ON COLUMN conversations.user_id IS '用户ID';
COMMENT ON COLUMN conversations.target_id IS '对方用户ID（单聊）';
COMMENT ON COLUMN conversations.group_id IS '群组ID（群聊）';
COMMENT ON COLUMN conversations.conversation_type IS '会话类型';
COMMENT ON COLUMN conversations.last_message_id IS '最后一条消息ID';
COMMENT ON COLUMN conversations.unread_count IS '未读消息数';
COMMENT ON COLUMN conversations.is_pinned IS '是否置顶';
COMMENT ON COLUMN conversations.is_muted IS '是否免打扰';
