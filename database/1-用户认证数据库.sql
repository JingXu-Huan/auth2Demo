-- ========================================================================
-- 用户认证与账户系统 (User & Auth Database)
-- ========================================================================

CREATE DATABASE auth_db;
\c auth_db;

-- 1.1 用户基础表 (核心用户信息)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    
    -- 用户状态
    status SMALLINT NOT NULL DEFAULT 1, -- 1:正常, 2:禁用, 3:锁定, 4:未激活
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    
    -- 安全相关
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(100),
    last_login_at TIMESTAMPTZ,
    last_login_ip INET,
    failed_login_count INT DEFAULT 0,
    locked_until TIMESTAMPTZ,
    
    -- 时间戳
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ -- 软删除
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_phone ON users(phone) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status);

-- 1.2 用户详情表 (扩展信息)
CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    nickname VARCHAR(50),
    real_name VARCHAR(50),
    avatar_url VARCHAR(500),
    gender SMALLINT, -- 1:男, 2:女, 0:未知
    birthday DATE,
    
    -- 工作信息
    company VARCHAR(100),
    department VARCHAR(100),
    position VARCHAR(100),
    employee_id VARCHAR(50),
    hire_date DATE,
    
    -- 地址信息
    country VARCHAR(50),
    province VARCHAR(50),
    city VARCHAR(50),
    address TEXT,
    
    -- 其他
    bio TEXT,
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'zh-CN',
    extra JSONB, -- 扩展字段
    
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 1.3 OAuth2 客户端表
CREATE TABLE oauth2_clients (
    id VARCHAR(100) PRIMARY KEY,
    client_secret VARCHAR(255) NOT NULL,
    client_name VARCHAR(100) NOT NULL,
    redirect_uris TEXT[], -- 数组类型
    grant_types VARCHAR(50)[] DEFAULT ARRAY['authorization_code'],
    response_types VARCHAR(50)[] DEFAULT ARRAY['code'],
    scope VARCHAR(500),
    
    -- 客户端配置
    access_token_validity INT DEFAULT 3600, -- 秒
    refresh_token_validity INT DEFAULT 2592000, -- 30天
    auto_approve BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 1.4 OAuth2 令牌表
CREATE TABLE oauth2_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id VARCHAR(100) NOT NULL REFERENCES oauth2_clients(id),
    
    access_token TEXT NOT NULL UNIQUE,
    refresh_token TEXT UNIQUE,
    token_type VARCHAR(20) DEFAULT 'Bearer',
    scope VARCHAR(500),
    
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- 设备信息
    device_id VARCHAR(100),
    device_type VARCHAR(50), -- web, mobile, desktop
    user_agent TEXT,
    ip_address INET
);

CREATE INDEX idx_oauth2_tokens_user_id ON oauth2_tokens(user_id);
CREATE INDEX idx_oauth2_tokens_expires_at ON oauth2_tokens(expires_at);

-- 1.5 登录日志表 (分区表)
CREATE TABLE login_logs (
    id BIGSERIAL,
    user_id BIGINT NOT NULL,
    login_type VARCHAR(20), -- password, oauth, sso, mfa
    success BOOLEAN NOT NULL,
    
    ip_address INET,
    user_agent TEXT,
    device_id VARCHAR(100),
    
    -- 地理位置
    country VARCHAR(50),
    city VARCHAR(50),
    
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (created_at, id)
) PARTITION BY RANGE (created_at);

-- 创建分区 (按月)
CREATE TABLE login_logs_2024_11 PARTITION OF login_logs
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE login_logs_2024_12 PARTITION OF login_logs
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- 1.6 多因素认证记录表
CREATE TABLE mfa_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    mfa_type VARCHAR(20), -- totp, sms, email
    success BOOLEAN NOT NULL,
    code VARCHAR(10),
    ip_address INET,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mfa_logs_user ON mfa_logs(user_id, created_at DESC);

-- 1.7 会话管理表
CREATE TABLE user_sessions (
    id VARCHAR(100) PRIMARY KEY, -- Session ID
    user_id BIGINT NOT NULL REFERENCES users(id),
    
    -- 会话信息
    ip_address INET,
    user_agent TEXT,
    device_id VARCHAR(100),
    device_type VARCHAR(50),
    
    -- 会话数据
    session_data JSONB,
    
    -- 时间管理
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_user_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_expires ON user_sessions(expires_at);

-- 1.8 密码重置表
CREATE TABLE password_resets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(100),
    
    used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMPTZ
);

CREATE INDEX idx_password_resets_token ON password_resets(token) WHERE used = FALSE;
CREATE INDEX idx_password_resets_user ON password_resets(user_id);

-- 1.9 密码历史表
CREATE TABLE password_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_history_user ON password_history(user_id, created_at DESC);

-- 1.10 第三方OAuth绑定表 (支持Gitee、GitHub、微信等)
CREATE TABLE oauth_bindings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- OAuth提供商信息
    provider VARCHAR(50) NOT NULL, -- gitee, github, wechat, qq, google, microsoft
    provider_user_id VARCHAR(100) NOT NULL, -- 第三方平台的用户ID
    provider_username VARCHAR(100), -- 第三方平台的用户名
    
    -- OAuth令牌
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMPTZ,
    
    -- 第三方用户信息（缓存）
    provider_email VARCHAR(255),
    provider_nickname VARCHAR(100),
    provider_avatar_url VARCHAR(500),
    provider_profile_url VARCHAR(500),
    provider_data JSONB, -- 存储完整的第三方用户信息
    
    -- 绑定状态
    is_primary BOOLEAN DEFAULT FALSE, -- 是否为主要登录方式
    bind_status SMALLINT DEFAULT 1, -- 1:正常, 2:已解绑, 3:禁用
    
    -- 时间戳
    bound_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, -- 绑定时间
    last_login_at TIMESTAMPTZ, -- 最后登录时间
    unbound_at TIMESTAMPTZ, -- 解绑时间
    
    UNIQUE(provider, provider_user_id)
);

CREATE INDEX idx_oauth_bindings_user ON oauth_bindings(user_id) WHERE bind_status = 1;
CREATE INDEX idx_oauth_bindings_provider ON oauth_bindings(provider, provider_user_id);

-- 1.11 OAuth应用配置表 (存储Gitee等平台的应用配置)
CREATE TABLE oauth_app_configs (
    id BIGSERIAL PRIMARY KEY,
    
    -- 应用标识
    provider VARCHAR(50) NOT NULL UNIQUE, -- gitee, github, wechat
    provider_name VARCHAR(100) NOT NULL, -- 显示名称：Gitee、GitHub、微信
    
    -- OAuth配置
    client_id VARCHAR(200) NOT NULL,
    client_secret VARCHAR(500) NOT NULL, -- 应加密存储
    
    -- OAuth URLs
    authorize_url VARCHAR(500) NOT NULL, -- 授权URL
    token_url VARCHAR(500) NOT NULL, -- 获取token URL
    user_info_url VARCHAR(500) NOT NULL, -- 获取用户信息URL
    revoke_url VARCHAR(500), -- 撤销授权URL（可选）
    
    -- 回调配置
    redirect_uri VARCHAR(500) NOT NULL, -- 回调地址
    scope VARCHAR(500), -- 请求的权限范围
    
    -- 状态
    enabled BOOLEAN DEFAULT TRUE,
    
    -- 其他配置
    extra_params JSONB, -- 额外的OAuth参数
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 插入Gitee OAuth配置（示例）
INSERT INTO oauth_app_configs (provider, provider_name, client_id, client_secret, 
    authorize_url, token_url, user_info_url, redirect_uri, scope) 
VALUES 
('gitee', 'Gitee', 'your_gitee_client_id', 'your_gitee_client_secret',
    'https://gitee.com/oauth/authorize', 
    'https://gitee.com/oauth/token',
    'https://gitee.com/api/v5/user',
    'http://localhost:8080/oauth/callback/gitee',
    'user_info emails'),
('github', 'GitHub', 'your_github_client_id', 'your_github_client_secret',
    'https://github.com/login/oauth/authorize',
    'https://github.com/login/oauth/access_token',
    'https://api.github.com/user',
    'http://localhost:8080/oauth/callback/github',
    'user:email'),
('wechat', '微信', 'your_wechat_appid', 'your_wechat_secret',
    'https://open.weixin.qq.com/connect/qrconnect',
    'https://api.weixin.qq.com/sns/oauth2/access_token',
    'https://api.weixin.qq.com/sns/userinfo',
    'http://localhost:8080/oauth/callback/wechat',
    'snsapi_login');

-- 1.12 OAuth登录日志表
CREATE TABLE oauth_login_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(100),
    
    -- 登录信息
    action VARCHAR(20) NOT NULL, -- authorize, callback, bind, unbind, login
    success BOOLEAN NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    
    -- 请求信息
    ip_address INET,
    user_agent TEXT,
    
    -- OAuth信息
    state_token VARCHAR(100), -- OAuth state参数
    code VARCHAR(200), -- OAuth授权码
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_oauth_login_logs_user ON oauth_login_logs(user_id);
CREATE INDEX idx_oauth_login_logs_provider ON oauth_login_logs(provider, provider_user_id);

-- 触发器：自动更新 updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON user_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
