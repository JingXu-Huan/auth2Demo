-- ========================================================================
-- 好友关系系统 (Relationship Database)
-- ========================================================================

CREATE DATABASE relationship_db;
\c relationship_db;

-- 3.1 好友申请表
CREATE TABLE friend_requests (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    
    -- 申请信息
    message TEXT,
    remark VARCHAR(50), -- 备注名
    
    -- 状态
    status SMALLINT DEFAULT 0, -- 0:待处理, 1:已同意, 2:已拒绝, 3:已忽略, 4:已过期
    
    -- 来源
    source VARCHAR(50), -- search, qrcode, group, recommend, card
    source_id VARCHAR(100), -- 群ID、推荐人ID等
    
    -- 处理信息
    handled_by BIGINT,
    handled_at TIMESTAMPTZ,
    reject_reason TEXT,
    
    -- 有效期
    expires_at TIMESTAMPTZ DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days'),
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_friend_request UNIQUE (sender_id, receiver_id)
);

CREATE INDEX idx_friend_requests_receiver ON friend_requests(receiver_id, status);
CREATE INDEX idx_friend_requests_sender ON friend_requests(sender_id);
CREATE INDEX idx_friend_requests_expires ON friend_requests(expires_at) WHERE status = 0;

-- 3.2 好友关系表
CREATE TABLE friend_relations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    
    -- 好友信息
    remark VARCHAR(50), -- 备注名
    tags TEXT[], -- 标签
    description TEXT, -- 描述
    
    -- 分组
    group_id BIGINT,
    
    -- 设置
    blocked BOOLEAN DEFAULT FALSE, -- 拉黑
    starred BOOLEAN DEFAULT FALSE, -- 星标好友
    stealth BOOLEAN DEFAULT FALSE, -- 对其隐身
    
    -- 权限设置
    allow_view_moments BOOLEAN DEFAULT TRUE, -- 允许查看朋友圈
    allow_view_online BOOLEAN DEFAULT TRUE, -- 允许查看在线状态
    
    -- 关系信息
    relationship_type SMALLINT DEFAULT 1, -- 1:普通好友, 2:亲密好友, 3:关注
    intimacy_score INT DEFAULT 0, -- 亲密度分数
    
    -- 来源
    source VARCHAR(50),
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    
    CONSTRAINT uk_friend_relation UNIQUE (user_id, friend_id)
);

CREATE INDEX idx_friend_relations_user ON friend_relations(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_friend_relations_friend ON friend_relations(friend_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_friend_relations_blocked ON friend_relations(user_id) WHERE blocked = TRUE;
CREATE INDEX idx_friend_relations_starred ON friend_relations(user_id) WHERE starred = TRUE;

-- 3.3 好友分组表
CREATE TABLE friend_groups (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    
    -- 分组设置
    is_default BOOLEAN DEFAULT FALSE, -- 是否默认分组
    sort_order INT DEFAULT 0,
    member_count INT DEFAULT 0,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_friend_groups_user ON friend_groups(user_id);
CREATE UNIQUE INDEX idx_friend_groups_default ON friend_groups(user_id) WHERE is_default = TRUE;

-- 3.4 黑名单表（独立维护）
CREATE TABLE blacklist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    blocked_user_id BIGINT NOT NULL,
    
    reason TEXT,
    
    -- 屏蔽范围
    block_messages BOOLEAN DEFAULT TRUE,
    block_calls BOOLEAN DEFAULT TRUE,
    block_moments BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_blacklist UNIQUE (user_id, blocked_user_id)
);

CREATE INDEX idx_blacklist_user ON blacklist(user_id);
CREATE INDEX idx_blacklist_blocked ON blacklist(blocked_user_id);

-- 3.5 关注关系表（单向关系）
CREATE TABLE follows (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL, -- 关注者
    following_id BIGINT NOT NULL, -- 被关注者
    
    -- 关注设置
    special_follow BOOLEAN DEFAULT FALSE, -- 特别关注
    notifications_enabled BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_follow UNIQUE (follower_id, following_id)
);

CREATE INDEX idx_follows_follower ON follows(follower_id);
CREATE INDEX idx_follows_following ON follows(following_id);

-- 3.6 好友互动记录表
CREATE TABLE friend_interactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    
    -- 互动统计
    message_count INT DEFAULT 0,
    call_count INT DEFAULT 0,
    call_duration INT DEFAULT 0, -- 总通话时长（秒）
    
    -- 最近互动
    last_message_at TIMESTAMPTZ,
    last_call_at TIMESTAMPTZ,
    last_interaction_at TIMESTAMPTZ,
    
    -- 亲密度计算因子
    interaction_score INT DEFAULT 0,
    
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_friend_interactions_unique ON friend_interactions(user_id, friend_id);
CREATE INDEX idx_friend_interactions_score ON friend_interactions(user_id, interaction_score DESC);

-- 3.7 好友推荐表
CREATE TABLE friend_recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recommended_user_id BIGINT NOT NULL,
    
    -- 推荐原因
    reason VARCHAR(100), -- mutual_friends, same_group, nearby
    reason_detail JSONB, -- 具体信息（共同好友列表等）
    
    -- 推荐分数
    score FLOAT DEFAULT 0,
    
    -- 用户反馈
    feedback SMALLINT, -- 1:感兴趣, 2:不感兴趣, 3:已添加
    feedback_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days')
);

CREATE INDEX idx_friend_recommendations_user ON friend_recommendations(user_id, score DESC);
CREATE INDEX idx_friend_recommendations_expires ON friend_recommendations(expires_at);

-- 3.8 共同好友缓存表
CREATE TABLE mutual_friends_cache (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    
    mutual_friend_ids BIGINT[],
    mutual_count INT DEFAULT 0,
    
    calculated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days'),
    
    CONSTRAINT uk_mutual_friends UNIQUE (user1_id, user2_id)
);

CREATE INDEX idx_mutual_friends_users ON mutual_friends_cache(user1_id, user2_id);
CREATE INDEX idx_mutual_friends_expires ON mutual_friends_cache(expires_at);

-- 触发器：更新好友分组成员数
CREATE OR REPLACE FUNCTION update_friend_group_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE friend_groups 
        SET member_count = member_count + 1 
        WHERE id = NEW.group_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE friend_groups 
        SET member_count = member_count - 1 
        WHERE id = OLD.group_id;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.group_id IS DISTINCT FROM NEW.group_id THEN
            UPDATE friend_groups 
            SET member_count = member_count - 1 
            WHERE id = OLD.group_id;
            UPDATE friend_groups 
            SET member_count = member_count + 1 
            WHERE id = NEW.group_id;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_group_count_trigger
AFTER INSERT OR UPDATE OR DELETE ON friend_relations
FOR EACH ROW EXECUTE FUNCTION update_friend_group_count();
