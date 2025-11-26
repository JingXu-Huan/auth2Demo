-- ========================================================================
-- 文件存储系统 (File Storage Database) - CAS内容寻址存储
-- ========================================================================

CREATE DATABASE file_db;
\c file_db;

-- 4.1 物理文件元数据表 (CAS设计 - 核心表)
CREATE TABLE file_metadata (
    file_hash VARCHAR(64) PRIMARY KEY, -- SHA-256哈希值
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50), -- 文件类型：image, video, document, audio
    mime_type VARCHAR(100),
    ext VARCHAR(20), -- 扩展名
    
    -- 存储信息
    storage_backend VARCHAR(20) DEFAULT 'minio', -- minio, s3, oss, local
    bucket_name VARCHAR(100),
    object_key VARCHAR(500) NOT NULL, -- CAS路径: cas/a1/b2/hash.ext
    
    -- 引用计数（垃圾回收）
    ref_count INT DEFAULT 1,
    
    -- 缩略图与预览
    thumbnail_keys JSONB, -- {"small": "path", "medium": "path", "large": "path"}
    preview_key VARCHAR(500), -- 预览文件路径
    
    -- 媒体元信息
    width INT, -- 图片/视频宽度
    height INT, -- 图片/视频高度
    duration INT, -- 音频/视频时长(秒)
    bitrate INT, -- 比特率
    
    -- 文档信息
    page_count INT, -- 页数
    word_count INT, -- 字数
    
    -- 安全检测
    virus_scan_status SMALLINT DEFAULT 0, -- 0:未检测, 1:安全, 2:病毒
    virus_scan_result TEXT,
    content_audit_status SMALLINT DEFAULT 0, -- 0:未审核, 1:通过, 2:违规
    content_audit_result JSONB,
    
    -- 扩展信息
    extra JSONB, -- 其他元信息
    
    status SMALLINT DEFAULT 1, -- 1:正常, 2:待删除, 3:已删除
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_file_metadata_status ON file_metadata(status);
CREATE INDEX idx_file_metadata_ref_count ON file_metadata(ref_count) WHERE ref_count <= 0;
CREATE INDEX idx_file_metadata_type ON file_metadata(file_type);

-- 4.2 用户文件关联表
CREATE TABLE user_files (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_hash VARCHAR(64) NOT NULL REFERENCES file_metadata(file_hash),
    
    -- 文件信息
    original_name VARCHAR(255) NOT NULL, -- 原始文件名
    display_name VARCHAR(255), -- 显示名称
    file_path VARCHAR(500), -- 用户视角的虚拟路径
    folder_id BIGINT, -- 所属文件夹
    
    -- 来源
    source_type VARCHAR(20), -- message, document, avatar, upload
    source_id VARCHAR(100), -- 消息ID、文档ID等
    
    -- 权限
    is_public BOOLEAN DEFAULT FALSE,
    share_token VARCHAR(100) UNIQUE, -- 分享链接token
    share_password VARCHAR(20), -- 分享密码
    share_expires_at TIMESTAMPTZ,
    download_count INT DEFAULT 0,
    
    -- 标签与分类
    tags TEXT[],
    category VARCHAR(50),
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ -- 软删除
);

CREATE INDEX idx_user_files_user ON user_files(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_files_hash ON user_files(file_hash);
CREATE INDEX idx_user_files_folder ON user_files(folder_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_files_share ON user_files(share_token) WHERE share_token IS NOT NULL;

-- 4.3 文件夹表
CREATE TABLE folders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    parent_id BIGINT REFERENCES folders(id) ON DELETE CASCADE,
    
    name VARCHAR(255) NOT NULL,
    path VARCHAR(1000), -- 完整路径: /root/folder1/folder2
    level INT DEFAULT 1,
    
    -- 统计
    file_count INT DEFAULT 0,
    folder_count INT DEFAULT 0,
    total_size BIGINT DEFAULT 0,
    
    -- 设置
    is_shared BOOLEAN DEFAULT FALSE,
    color VARCHAR(7), -- 文件夹颜色
    icon VARCHAR(50), -- 图标
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_folders_user ON folders(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_folders_parent ON folders(parent_id);
CREATE INDEX idx_folders_path ON folders(path);

-- 4.4 文件分片上传表
CREATE TABLE file_upload_sessions (
    id BIGSERIAL PRIMARY KEY,
    upload_id VARCHAR(100) NOT NULL UNIQUE, -- 上传会话ID
    user_id BIGINT NOT NULL,
    
    -- 文件信息
    file_hash VARCHAR(64),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    
    -- 分片信息
    chunk_size INT NOT NULL, -- 每个分片大小
    chunk_count INT NOT NULL, -- 总分片数
    uploaded_chunks INT DEFAULT 0, -- 已上传分片数
    chunk_etags JSONB, -- 各分片的ETag {"1": "etag1", "2": "etag2"}
    
    -- MinIO Multipart Upload
    minio_upload_id VARCHAR(200), -- MinIO的uploadId
    
    -- 状态
    status SMALLINT DEFAULT 1, -- 1:上传中, 2:合并中, 3:完成, 4:失败, 5:已取消
    error_message TEXT,
    
    expires_at TIMESTAMPTZ DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours'),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_upload_sessions_upload_id ON file_upload_sessions(upload_id);
CREATE INDEX idx_upload_sessions_user ON file_upload_sessions(user_id) WHERE status = 1;
CREATE INDEX idx_upload_sessions_expires ON file_upload_sessions(expires_at) WHERE status = 1;

-- 4.5 文件分享记录表
CREATE TABLE file_shares (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL REFERENCES user_files(id),
    shared_by BIGINT NOT NULL,
    
    -- 分享设置
    share_type SMALLINT DEFAULT 1, -- 1:链接分享, 2:指定用户, 3:群组分享
    share_targets BIGINT[], -- 目标用户或群组ID
    
    -- 权限
    allow_download BOOLEAN DEFAULT TRUE,
    allow_preview BOOLEAN DEFAULT TRUE,
    allow_save BOOLEAN DEFAULT FALSE,
    
    -- 统计
    view_count INT DEFAULT 0,
    download_count INT DEFAULT 0,
    save_count INT DEFAULT 0,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_file_shares_file ON file_shares(file_id);
CREATE INDEX idx_file_shares_user ON file_shares(shared_by);

-- 4.6 文件操作日志表
CREATE TABLE file_operation_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id BIGINT,
    file_hash VARCHAR(64),
    
    operation VARCHAR(50) NOT NULL, -- upload, download, delete, share, rename
    
    -- 操作详情
    details JSONB,
    
    -- 客户端信息
    ip_address INET,
    user_agent TEXT,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE file_operation_logs_2024_11 PARTITION OF file_operation_logs
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_file_op_logs_user ON file_operation_logs(user_id);
CREATE INDEX idx_file_op_logs_file ON file_operation_logs(file_id);

-- 4.7 用户存储配额表
CREATE TABLE user_storage_quotas (
    user_id BIGINT PRIMARY KEY,
    
    -- 配额设置
    max_storage BIGINT DEFAULT 10737418240, -- 默认10GB
    max_file_size BIGINT DEFAULT 524288000, -- 默认500MB
    
    -- 使用情况
    used_storage BIGINT DEFAULT 0,
    file_count INT DEFAULT 0,
    
    -- 特殊配额
    extra_storage BIGINT DEFAULT 0, -- 额外赠送空间
    expires_at TIMESTAMPTZ, -- 配额过期时间
    
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 4.8 文件转码任务表
CREATE TABLE file_transcode_tasks (
    id BIGSERIAL PRIMARY KEY,
    file_hash VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    
    -- 转码设置
    source_format VARCHAR(20),
    target_format VARCHAR(20),
    quality VARCHAR(20), -- high, medium, low
    parameters JSONB, -- 转码参数
    
    -- 输出文件
    output_hash VARCHAR(64),
    output_key VARCHAR(500),
    
    -- 状态
    status SMALLINT DEFAULT 0, -- 0:待处理, 1:处理中, 2:成功, 3:失败
    progress INT DEFAULT 0, -- 进度百分比
    error_message TEXT,
    
    -- 执行信息
    worker_id VARCHAR(50),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transcode_tasks_status ON file_transcode_tasks(status);
CREATE INDEX idx_transcode_tasks_user ON file_transcode_tasks(user_id);

-- 触发器：自动更新用户存储使用量
CREATE OR REPLACE FUNCTION update_user_storage()
RETURNS TRIGGER AS $$
DECLARE
    v_file_size BIGINT;
BEGIN
    SELECT file_size INTO v_file_size FROM file_metadata WHERE file_hash = NEW.file_hash;
    
    IF TG_OP = 'INSERT' THEN
        INSERT INTO user_storage_quotas (user_id, used_storage, file_count)
        VALUES (NEW.user_id, v_file_size, 1)
        ON CONFLICT (user_id) DO UPDATE
        SET used_storage = user_storage_quotas.used_storage + v_file_size,
            file_count = user_storage_quotas.file_count + 1,
            updated_at = CURRENT_TIMESTAMP;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE user_storage_quotas
        SET used_storage = used_storage - v_file_size,
            file_count = file_count - 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = OLD.user_id;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_storage_trigger
AFTER INSERT OR DELETE ON user_files
FOR EACH ROW EXECUTE FUNCTION update_user_storage();
