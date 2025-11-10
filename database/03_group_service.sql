-- ============================================
-- 群组服务数据库 (aio_group)
-- ============================================

CREATE DATABASE aio_group;

\c aio_group;

-- ============================================
-- 1. 群组表 (groups)
-- ============================================
CREATE TABLE groups (
    group_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    avatar VARCHAR(500),
    owner_id BIGINT NOT NULL,
    member_count INT DEFAULT 0,
    max_members INT DEFAULT 500,
    join_type VARCHAR(20) DEFAULT 'FREE' CHECK (join_type IN ('FREE', 'APPROVAL', 'INVITE_ONLY')),
    announcement TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_groups_owner ON groups(owner_id);
CREATE INDEX idx_groups_created_at ON groups(created_at);

COMMENT ON TABLE groups IS '群组表';

-- ============================================
-- 2. 群组成员表 (group_members)
-- ============================================
CREATE TABLE group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id VARCHAR(50) NOT NULL REFERENCES groups(group_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER' CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    nickname VARCHAR(50),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_group_members_group ON group_members(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);
CREATE INDEX idx_group_members_role ON group_members(role);

COMMENT ON TABLE group_members IS '群组成员表';
