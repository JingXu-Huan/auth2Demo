-- ========================================================================
-- 文档协同系统 (Document Collaboration Database) - 基于CRDT/Yjs
-- ========================================================================

CREATE DATABASE doc_db;
\c doc_db;

-- 5.1 文档表（主表）
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    doc_id VARCHAR(100) UNIQUE NOT NULL, -- 文档唯一标识
    
    -- 基础信息
    title VARCHAR(255) NOT NULL,
    doc_type VARCHAR(20) NOT NULL, -- text, spreadsheet, slide, canvas
    template_id BIGINT, -- 模板ID
    
    -- 所有权与组织
    owner_id BIGINT NOT NULL,
    team_id BIGINT, -- 团队ID
    space_id BIGINT, -- 空间ID
    folder_id BIGINT, -- 文件夹ID
    
    -- 内容存储
    content_version BIGINT DEFAULT 1,
    content_size BIGINT DEFAULT 0,
    
    -- 内容存储策略（大文档存MinIO）
    storage_type VARCHAR(20) DEFAULT 'database', -- database, minio
    content BYTEA, -- 小文档直接存储
    content_url VARCHAR(500), -- MinIO URL for large docs
    
    -- CRDT状态
    yjs_state BYTEA, -- Yjs文档状态
    vector_clock JSONB, -- 向量时钟
    
    -- 协同锁（可选的悲观锁）
    lock_user_id BIGINT,
    lock_token VARCHAR(100),
    lock_expires_at TIMESTAMPTZ,
    
    -- 统计
    view_count INT DEFAULT 0,
    edit_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    star_count INT DEFAULT 0,
    
    -- 权限设置
    visibility VARCHAR(20) DEFAULT 'private', -- private, team, public
    allow_comment BOOLEAN DEFAULT TRUE,
    allow_copy BOOLEAN DEFAULT TRUE,
    allow_download BOOLEAN DEFAULT TRUE,
    
    -- SEO与分享
    slug VARCHAR(100), -- 自定义URL
    cover_image VARCHAR(500),
    summary TEXT,
    
    -- 状态
    status SMALLINT DEFAULT 1, -- 1:正常, 2:归档, 3:回收站, 4:已删除
    published BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_documents_owner ON documents(owner_id);
CREATE INDEX idx_documents_team ON documents(team_id);
CREATE INDEX idx_documents_folder ON documents(folder_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_slug ON documents(slug) WHERE slug IS NOT NULL;

-- 5.2 文档版本表
CREATE TABLE document_versions (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL REFERENCES documents(id),
    version_number BIGINT NOT NULL,
    
    -- 版本信息
    title VARCHAR(255),
    description TEXT, -- 版本说明
    
    -- 内容
    content BYTEA,
    content_url VARCHAR(500),
    content_hash VARCHAR(64),
    content_size BIGINT,
    
    -- CRDT快照
    yjs_snapshot BYTEA,
    
    -- 变更信息
    author_id BIGINT NOT NULL,
    change_summary TEXT,
    diff_data JSONB, -- 与上一版本的差异
    
    -- 标签
    is_major BOOLEAN DEFAULT FALSE, -- 是否主要版本
    is_published BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(doc_id, version_number)
);

CREATE INDEX idx_doc_versions_doc ON document_versions(doc_id, version_number DESC);

-- 5.3 文档协同会话表（实时协同）
CREATE TABLE doc_collaboration_sessions (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL REFERENCES documents(id),
    user_id BIGINT NOT NULL,
    
    -- 会话信息
    session_token VARCHAR(100) UNIQUE NOT NULL,
    
    -- WebSocket连接信息
    connection_id VARCHAR(100) UNIQUE,
    node_id VARCHAR(50), -- 处理节点（Yjs服务节点）
    
    -- 用户状态
    user_name VARCHAR(50),
    user_color VARCHAR(7), -- 用户光标颜色
    
    -- 光标与选区
    cursor_position JSONB, -- {"index": 100, "length": 0}
    selection_range JSONB, -- {"index": 50, "length": 20}
    viewport JSONB, -- 可视区域
    
    -- 状态
    is_active BOOLEAN DEFAULT TRUE,
    is_editing BOOLEAN DEFAULT FALSE,
    last_activity_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_heartbeat_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    joined_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ
);

CREATE INDEX idx_collab_sessions_doc ON doc_collaboration_sessions(doc_id) WHERE is_active = TRUE;
CREATE INDEX idx_collab_sessions_user ON doc_collaboration_sessions(user_id) WHERE is_active = TRUE;

-- 5.4 文档操作日志表 (CRDT操作流)
CREATE TABLE doc_operations (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    session_id BIGINT,
    user_id BIGINT NOT NULL,
    
    -- 操作信息
    operation_id VARCHAR(100) UNIQUE NOT NULL, -- 操作唯一ID
    parent_operation_id VARCHAR(100), -- 父操作ID（用于依赖）
    
    -- CRDT操作
    operation_type VARCHAR(20), -- insert, delete, format, style
    
    -- Yjs Update (二进制)
    yjs_update BYTEA NOT NULL,
    
    -- 操作元数据
    position INT, -- 操作位置
    length INT, -- 影响长度
    content TEXT, -- 操作内容预览
    
    -- 向量时钟
    vector_clock JSONB,
    lamport_timestamp BIGINT,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE doc_operations_2024_11 PARTITION OF doc_operations
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_doc_operations_doc ON doc_operations(doc_id, created_at);
CREATE INDEX idx_doc_operations_session ON doc_operations(session_id);

-- 5.5 文档评论表
CREATE TABLE doc_comments (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL REFERENCES documents(id),
    parent_id BIGINT REFERENCES doc_comments(id), -- 父评论（回复）
    
    -- 评论者
    user_id BIGINT NOT NULL,
    user_name VARCHAR(50),
    
    -- 评论内容
    content TEXT NOT NULL,
    
    -- 位置锚点（针对特定文本的评论）
    anchor_start INT, -- 锚点起始位置
    anchor_end INT, -- 锚点结束位置
    anchor_text TEXT, -- 锚点文本快照
    
    -- 反应
    like_count INT DEFAULT 0,
    reply_count INT DEFAULT 0,
    
    -- 状态
    is_resolved BOOLEAN DEFAULT FALSE,
    is_pinned BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_doc_comments_doc ON doc_comments(doc_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_doc_comments_parent ON doc_comments(parent_id);

-- 5.6 文档权限表
CREATE TABLE doc_permissions (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL REFERENCES documents(id),
    
    -- 权限主体
    grantee_type VARCHAR(20) NOT NULL, -- user, team, role, public
    grantee_id BIGINT, -- 用户/团队/角色ID
    
    -- 权限级别
    permission_level VARCHAR(20) NOT NULL, -- view, comment, edit, admin
    
    -- 权限详情
    can_view BOOLEAN DEFAULT TRUE,
    can_comment BOOLEAN DEFAULT FALSE,
    can_edit BOOLEAN DEFAULT FALSE,
    can_delete BOOLEAN DEFAULT FALSE,
    can_share BOOLEAN DEFAULT FALSE,
    
    -- 有效期
    expires_at TIMESTAMPTZ,
    
    -- 授权信息
    granted_by BIGINT NOT NULL,
    granted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(doc_id, grantee_type, grantee_id)
);

CREATE INDEX idx_doc_permissions_doc ON doc_permissions(doc_id);
CREATE INDEX idx_doc_permissions_grantee ON doc_permissions(grantee_type, grantee_id);

-- 5.7 文档模板表
CREATE TABLE doc_templates (
    id BIGSERIAL PRIMARY KEY,
    
    -- 模板信息
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50), -- 分类
    
    -- 模板内容
    doc_type VARCHAR(20) NOT NULL,
    content BYTEA,
    content_url VARCHAR(500),
    
    -- 预览
    preview_image VARCHAR(500),
    
    -- 使用统计
    use_count INT DEFAULT 0,
    
    -- 权限
    is_public BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doc_templates_category ON doc_templates(category);
CREATE INDEX idx_doc_templates_public ON doc_templates(is_public);

-- 5.8 文档收藏表
CREATE TABLE doc_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL REFERENCES documents(id),
    
    -- 收藏信息
    folder_name VARCHAR(50), -- 收藏夹
    notes TEXT, -- 备注
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, doc_id)
);

CREATE INDEX idx_doc_favorites_user ON doc_favorites(user_id);
CREATE INDEX idx_doc_favorites_doc ON doc_favorites(doc_id);

-- 5.9 文档活动流表
CREATE TABLE doc_activities (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL REFERENCES documents(id),
    user_id BIGINT NOT NULL,
    
    -- 活动类型
    activity_type VARCHAR(50) NOT NULL, -- create, edit, comment, share, view
    
    -- 活动详情
    details JSONB,
    
    -- 变更摘要
    change_summary TEXT,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE doc_activities_2024_11 PARTITION OF doc_activities
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_doc_activities_doc ON doc_activities(doc_id, created_at DESC);
CREATE INDEX idx_doc_activities_user ON doc_activities(user_id);

-- 5.10 文档空间表（文档集合）
CREATE TABLE doc_spaces (
    id BIGSERIAL PRIMARY KEY,
    
    -- 空间信息
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    
    -- 所有权
    owner_id BIGINT,
    team_id BIGINT,
    
    -- 配置
    is_public BOOLEAN DEFAULT FALSE,
    allow_guest BOOLEAN DEFAULT FALSE,
    
    -- 统计
    doc_count INT DEFAULT 0,
    member_count INT DEFAULT 0,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doc_spaces_owner ON doc_spaces(owner_id);
CREATE INDEX idx_doc_spaces_team ON doc_spaces(team_id);

-- 触发器：自动更新文档更新时间
CREATE OR REPLACE FUNCTION update_document_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_doc_updated_at BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_document_updated_at();
