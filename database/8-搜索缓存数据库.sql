-- ========================================================================
-- 搜索与缓存系统 (Search & Cache Database)
-- 支持Elasticsearch同步、Redis缓存管理、热点数据等
-- ========================================================================

CREATE DATABASE search_db;
\c search_db;

-- 8.1 搜索索引同步队列表
CREATE TABLE search_index_queue (
    id BIGSERIAL PRIMARY KEY,
    
    -- 索引目标
    index_name VARCHAR(50) NOT NULL, -- messages, documents, users, files
    document_type VARCHAR(50), -- 文档类型（ES7已弃用，保留兼容）
    document_id VARCHAR(100) NOT NULL,
    
    -- 操作
    operation VARCHAR(20) NOT NULL, -- index, update, delete, bulk
    
    -- 数据
    document_data JSONB, -- 要索引的数据
    
    -- 优先级
    priority SMALLINT DEFAULT 5, -- 1-10，数字越小优先级越高
    
    -- 处理状态
    status SMALLINT DEFAULT 0, -- 0:待处理, 1:处理中, 2:成功, 3:失败
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 3,
    
    -- 错误信息
    error_message TEXT,
    
    -- 时间
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ
);

CREATE INDEX idx_search_queue_status ON search_index_queue(status, priority) 
    WHERE status IN (0, 3);
CREATE INDEX idx_search_queue_index ON search_index_queue(index_name);
CREATE INDEX idx_search_queue_created ON search_index_queue(created_at);

-- 8.2 搜索历史表
CREATE TABLE search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- 搜索信息
    search_type VARCHAR(50), -- message, document, file, user
    search_query TEXT NOT NULL,
    search_filters JSONB, -- 过滤条件
    
    -- 搜索结果
    result_count INT,
    result_ids TEXT[], -- 前N个结果ID
    
    -- 性能指标
    search_time_ms INT,
    
    -- 用户行为
    clicked_results JSONB, -- 点击的结果
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE search_history_2024_11 PARTITION OF search_history
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_search_history_user ON search_history(user_id);
CREATE INDEX idx_search_history_query ON search_history(search_query);

-- 8.3 热词表
CREATE TABLE hot_keywords (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL,
    
    -- 统计
    search_count INT DEFAULT 1,
    user_count INT DEFAULT 1, -- 搜索的用户数
    
    -- 分类
    category VARCHAR(50),
    
    -- 时间窗口
    time_window VARCHAR(20), -- hour, day, week, month
    window_start TIMESTAMPTZ,
    
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(keyword, time_window, window_start)
);

CREATE INDEX idx_hot_keywords_count ON hot_keywords(search_count DESC);
CREATE INDEX idx_hot_keywords_window ON hot_keywords(time_window, window_start);

-- 8.4 搜索建议表
CREATE TABLE search_suggestions (
    id BIGSERIAL PRIMARY KEY,
    
    -- 建议内容
    query_prefix VARCHAR(100) NOT NULL, -- 查询前缀
    suggestion VARCHAR(200) NOT NULL, -- 建议词
    
    -- 类型
    suggestion_type VARCHAR(20), -- history, popular, semantic
    
    -- 权重
    weight FLOAT DEFAULT 1.0,
    
    -- 使用统计
    use_count INT DEFAULT 0,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_suggestions_prefix ON search_suggestions(query_prefix);
CREATE INDEX idx_suggestions_weight ON search_suggestions(weight DESC);

-- 8.5 缓存管理表
CREATE TABLE cache_metadata (
    cache_key VARCHAR(200) PRIMARY KEY,
    
    -- 缓存信息
    cache_type VARCHAR(50), -- redis, local, distributed
    cache_group VARCHAR(50), -- 缓存分组
    
    -- 数据信息
    data_size INT, -- 数据大小（字节）
    data_version INT DEFAULT 1,
    
    -- TTL管理
    ttl_seconds INT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    
    -- 统计
    hit_count INT DEFAULT 0,
    miss_count INT DEFAULT 0,
    last_accessed_at TIMESTAMPTZ
);

CREATE INDEX idx_cache_metadata_group ON cache_metadata(cache_group);
CREATE INDEX idx_cache_metadata_expires ON cache_metadata(expires_at);

-- 8.6 缓存预热配置表
CREATE TABLE cache_warmup_configs (
    id BIGSERIAL PRIMARY KEY,
    
    -- 配置信息
    name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- 预热目标
    target_type VARCHAR(50), -- user_profile, channel_info, hot_messages
    target_query TEXT, -- SQL或查询条件
    
    -- 执行计划
    schedule_type VARCHAR(20), -- startup, cron, manual
    cron_expression VARCHAR(100),
    
    -- 配置
    batch_size INT DEFAULT 100,
    parallel_degree INT DEFAULT 4,
    
    -- 状态
    enabled BOOLEAN DEFAULT TRUE,
    last_run_at TIMESTAMPTZ,
    last_run_status VARCHAR(20),
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_warmup_configs_enabled ON cache_warmup_configs(enabled);

-- 8.7 全文搜索索引表（PostgreSQL内置）
CREATE TABLE fulltext_index (
    id BIGSERIAL PRIMARY KEY,
    
    -- 文档信息
    resource_type VARCHAR(50) NOT NULL, -- message, document, comment
    resource_id VARCHAR(100) NOT NULL,
    
    -- 全文内容
    title TEXT,
    content TEXT NOT NULL,
    
    -- 全文搜索向量
    title_tsv tsvector,
    content_tsv tsvector,
    
    -- 元数据
    metadata JSONB,
    
    -- 权限
    owner_id BIGINT,
    visibility VARCHAR(20) DEFAULT 'private',
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(resource_type, resource_id)
);

-- 创建全文搜索索引
CREATE INDEX idx_fulltext_title_tsv ON fulltext_index USING GIN (title_tsv);
CREATE INDEX idx_fulltext_content_tsv ON fulltext_index USING GIN (content_tsv);
CREATE INDEX idx_fulltext_resource ON fulltext_index(resource_type, resource_id);

-- 触发器：自动更新全文搜索向量
CREATE OR REPLACE FUNCTION update_fulltext_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.title_tsv := to_tsvector('chinese', COALESCE(NEW.title, ''));
    NEW.content_tsv := to_tsvector('chinese', NEW.content);
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_fulltext_vector_trigger
BEFORE INSERT OR UPDATE ON fulltext_index
FOR EACH ROW EXECUTE FUNCTION update_fulltext_search_vector();

-- 8.8 搜索排序权重表
CREATE TABLE search_ranking_weights (
    id BIGSERIAL PRIMARY KEY,
    
    -- 排序因子
    factor_name VARCHAR(50) NOT NULL, -- relevance, recency, popularity, user_preference
    resource_type VARCHAR(50) NOT NULL,
    
    -- 权重配置
    weight FLOAT NOT NULL DEFAULT 1.0,
    
    -- 计算公式
    formula TEXT, -- 自定义计算公式
    
    -- 状态
    enabled BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(factor_name, resource_type)
);

-- 8.9 用户搜索偏好表
CREATE TABLE user_search_preferences (
    user_id BIGINT PRIMARY KEY,
    
    -- 搜索偏好
    preferred_types TEXT[], -- 偏好的内容类型
    preferred_sources TEXT[], -- 偏好的来源
    
    -- 过滤设置
    default_filters JSONB,
    
    -- 排序偏好
    sort_preference VARCHAR(50), -- relevance, time, popularity
    
    -- 历史分析
    common_keywords TEXT[], -- 常用关键词
    search_patterns JSONB, -- 搜索模式分析
    
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 8.10 实时热点表
CREATE TABLE realtime_trending (
    id BIGSERIAL PRIMARY KEY,
    
    -- 热点内容
    trending_type VARCHAR(50) NOT NULL, -- keyword, topic, document, user
    trending_id VARCHAR(100) NOT NULL,
    trending_title VARCHAR(200),
    
    -- 统计指标
    score FLOAT NOT NULL, -- 热度分数
    view_count INT DEFAULT 0,
    interaction_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    
    -- 时间窗口
    time_window VARCHAR(20) NOT NULL, -- 1h, 6h, 24h, 7d
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    
    -- 分类
    category VARCHAR(50),
    tags TEXT[],
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(trending_type, trending_id, time_window, window_start)
);

CREATE INDEX idx_trending_score ON realtime_trending(time_window, score DESC);
CREATE INDEX idx_trending_window ON realtime_trending(window_start, window_end);

-- 8.11 搜索同义词表
CREATE TABLE search_synonyms (
    id BIGSERIAL PRIMARY KEY,
    
    -- 同义词组
    synonym_group VARCHAR(100) NOT NULL,
    word VARCHAR(100) NOT NULL,
    
    -- 类型
    synonym_type VARCHAR(20), -- exact, one_way
    
    -- 权重
    weight FLOAT DEFAULT 1.0,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(synonym_group, word)
);

CREATE INDEX idx_synonyms_word ON search_synonyms(word);
CREATE INDEX idx_synonyms_group ON search_synonyms(synonym_group);

-- 8.12 搜索停用词表
CREATE TABLE search_stopwords (
    word VARCHAR(50) PRIMARY KEY,
    language VARCHAR(20) DEFAULT 'chinese',
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 预置中文停用词
INSERT INTO search_stopwords (word) VALUES 
    ('的'), ('了'), ('和'), ('是'), ('就'), ('都'), ('而'), ('及'), 
    ('与'), ('或'), ('在'), ('有'), ('个'), ('为'), ('这'), ('那');

-- 函数：搜索消息
CREATE OR REPLACE FUNCTION search_messages(
    p_query TEXT,
    p_user_id BIGINT,
    p_limit INT DEFAULT 20,
    p_offset INT DEFAULT 0
)
RETURNS TABLE(
    message_id VARCHAR,
    content TEXT,
    rank FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        fi.resource_id AS message_id,
        fi.content,
        ts_rank(fi.content_tsv, plainto_tsquery('chinese', p_query)) AS rank
    FROM fulltext_index fi
    WHERE fi.resource_type = 'message'
        AND fi.content_tsv @@ plainto_tsquery('chinese', p_query)
        AND (fi.owner_id = p_user_id OR fi.visibility = 'public')
    ORDER BY rank DESC, fi.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- 函数：更新热词统计
CREATE OR REPLACE FUNCTION update_hot_keywords(
    p_keyword VARCHAR,
    p_time_window VARCHAR
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO hot_keywords (keyword, time_window, window_start, search_count)
    VALUES (
        p_keyword, 
        p_time_window, 
        date_trunc(p_time_window, CURRENT_TIMESTAMP), 
        1
    )
    ON CONFLICT (keyword, time_window, window_start) DO UPDATE
    SET search_count = hot_keywords.search_count + 1,
        updated_at = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;
