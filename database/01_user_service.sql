-- ============================================
-- OAuth2 认证系统 PostgreSQL 数据库建表语句
-- 版本: 3.0 (整合版)
-- 日期: 2025-11-11
-- 数据库: aio (使用现有数据库)
-- ============================================

-- 注意：使用现有的 aio 数据库，不创建新数据库
-- \c aio;

-- 删除已存在的表（按依赖关系倒序删除）
DROP TABLE IF EXISTS login_logs CASCADE;
DROP TABLE IF EXISTS password_history CASCADE;
DROP TABLE IF EXISTS user_credentials CASCADE;
DROP TABLE IF EXISTS user_devices CASCADE;
DROP TABLE IF EXISTS email_verification_codes CASCADE;
DROP TABLE IF EXISTS user_friends CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================
-- 1. 用户主表 (users)
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    
    -- 个人信息
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    signature VARCHAR(200),
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'UNKNOWN')),
    birthday DATE,
    location VARCHAR(100),
    
    -- 在线状态
    status VARCHAR(20) DEFAULT 'OFFLINE' CHECK (status IN ('ONLINE', 'OFFLINE', 'BUSY', 'AWAY')),
    
    -- 邮箱验证相关
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    confirmation_token VARCHAR(255),
    token_expiry TIMESTAMP,
    
    -- 登录相关
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(50),
    
    -- 账户状态
    enabled BOOLEAN DEFAULT TRUE,
    account_non_expired BOOLEAN DEFAULT TRUE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    credentials_non_expired BOOLEAN DEFAULT TRUE,
    lock_reason VARCHAR(255),
    locked_at TIMESTAMP,
    
    -- 时间戳
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 创建触发器
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 注释
COMMENT ON TABLE users IS '用户主表';
COMMENT ON COLUMN users.id IS '用户ID';
COMMENT ON COLUMN users.username IS '用户名';
COMMENT ON COLUMN users.display_name IS '显示名称';
COMMENT ON COLUMN users.email IS '邮箱';
COMMENT ON COLUMN users.phone IS '手机号';
COMMENT ON COLUMN users.email_verified IS '邮箱是否已验证';
COMMENT ON COLUMN users.enabled IS '账户是否启用';
COMMENT ON COLUMN users.account_non_locked IS '账户是否未锁定';
COMMENT ON COLUMN users.last_login_ip IS '最后登录IP';

-- ============================================
-- 2. 用户凭证表 (user_credentials) - 支持多种登录方式
-- ============================================
CREATE TABLE user_credentials (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255),
    password_hash VARCHAR(255),
    access_token TEXT,
    refresh_token TEXT,
    token_expiry TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_provider UNIQUE (user_id, provider),
    CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX idx_user_credentials_provider_user ON user_credentials(provider, provider_user_id);

-- 触发器
CREATE TRIGGER update_user_credentials_updated_at BEFORE UPDATE ON user_credentials
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 注释
COMMENT ON TABLE user_credentials IS '用户凭证表';
COMMENT ON COLUMN user_credentials.provider IS '认证提供商（email/gitee/github等）';
COMMENT ON COLUMN user_credentials.provider_user_id IS '第三方平台用户ID';
COMMENT ON COLUMN user_credentials.password_hash IS '密码哈希（仅email登录）';

-- ============================================
-- 3. 好友关系表 (user_friends)
-- ============================================
CREATE TABLE user_friends (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    remark VARCHAR(50),
    source VARCHAR(20) CHECK (source IN ('SEARCH', 'QR_CODE', 'PHONE', 'GROUP')),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED')),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, friend_id)
);

-- 索引
CREATE INDEX idx_friends_user_id ON user_friends(user_id);
CREATE INDEX idx_friends_friend_id ON user_friends(friend_id);
CREATE INDEX idx_friends_status ON user_friends(status);

-- 注释
COMMENT ON TABLE user_friends IS '好友关系表';
COMMENT ON COLUMN user_friends.user_id IS '用户ID';
COMMENT ON COLUMN user_friends.friend_id IS '好友ID';
COMMENT ON COLUMN user_friends.remark IS '备注名';
COMMENT ON COLUMN user_friends.source IS '添加来源：SEARCH-搜索，QR_CODE-二维码，PHONE-手机号，GROUP-群组';
COMMENT ON COLUMN user_friends.status IS '状态：PENDING-待确认，ACCEPTED-已接受，REJECTED-已拒绝，BLOCKED-已拉黑';
COMMENT ON COLUMN user_friends.message IS '好友申请消息';

-- ============================================
-- 4. 密码历史表 (password_history) - 防止重复使用旧密码
-- ============================================
CREATE TABLE password_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX idx_password_history_user_id ON password_history(user_id);
CREATE INDEX idx_password_history_created_at ON password_history(created_at);

-- 注释
COMMENT ON TABLE password_history IS '密码历史表';
COMMENT ON COLUMN password_history.user_id IS '用户ID';
COMMENT ON COLUMN password_history.password_hash IS '密码哈希值';
COMMENT ON COLUMN password_history.created_at IS '创建时间';

-- ============================================
-- 5. 登录日志表 (login_logs) - 审计和安全分析
-- ============================================
CREATE TABLE login_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(255),
    ip_address VARCHAR(50) NOT NULL,
    user_agent TEXT,
    login_status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(255),
    location VARCHAR(255),
    device_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_login_logs_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE SET NULL
);

-- 索引
CREATE INDEX idx_login_logs_user_id ON login_logs(user_id);
CREATE INDEX idx_login_logs_email ON login_logs(email);
CREATE INDEX idx_login_logs_ip_address ON login_logs(ip_address);
CREATE INDEX idx_login_logs_login_status ON login_logs(login_status);
CREATE INDEX idx_login_logs_created_at ON login_logs(created_at);

-- 注释
COMMENT ON TABLE login_logs IS '登录日志表';
COMMENT ON COLUMN login_logs.login_status IS '登录状态（SUCCESS/FAILED/BLOCKED）';
COMMENT ON COLUMN login_logs.failure_reason IS '失败原因';
COMMENT ON COLUMN login_logs.location IS '登录地点（可选）';
COMMENT ON COLUMN login_logs.device_type IS '设备类型（PC/Mobile/Tablet）';

-- ============================================
-- 6. 设备管理表 (user_devices)
-- ============================================
CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    device_type VARCHAR(20) NOT NULL CHECK (device_type IN ('WEB', 'IOS', 'ANDROID', 'PC')),
    device_name VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent TEXT,
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, device_id),
    CONSTRAINT fk_devices_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX idx_devices_user_id ON user_devices(user_id);
CREATE INDEX idx_devices_device_id ON user_devices(device_id);
CREATE INDEX idx_devices_last_active ON user_devices(last_active_at);

-- 注释
COMMENT ON TABLE user_devices IS '用户设备表';
COMMENT ON COLUMN user_devices.user_id IS '用户ID';
COMMENT ON COLUMN user_devices.device_id IS '设备唯一标识';
COMMENT ON COLUMN user_devices.device_type IS '设备类型';
COMMENT ON COLUMN user_devices.device_name IS '设备名称';
COMMENT ON COLUMN user_devices.last_active_at IS '最后活跃时间';

-- ============================================
-- 7. 邮箱验证码表 (email_verification_codes)
-- ============================================
CREATE TABLE email_verification_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('REGISTER', 'RESET_PASSWORD', 'VERIFY_EMAIL')),
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_verification_email ON email_verification_codes(email);
CREATE INDEX idx_verification_code ON email_verification_codes(code);
CREATE INDEX idx_verification_expires ON email_verification_codes(expires_at);

-- 注释
COMMENT ON TABLE email_verification_codes IS '邮箱验证码表';
COMMENT ON COLUMN email_verification_codes.email IS '邮箱';
COMMENT ON COLUMN email_verification_codes.code IS '验证码';
COMMENT ON COLUMN email_verification_codes.type IS '类型';
COMMENT ON COLUMN email_verification_codes.expires_at IS '过期时间';
COMMENT ON COLUMN email_verification_codes.used IS '是否已使用';

-- ============================================
-- 插入测试数据
-- ============================================

-- 插入管理员用户（密码: Admin@123）
INSERT INTO users (
    username, 
    display_name, 
    email, 
    email_verified, 
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired
) VALUES (
    'admin', 
    '系统管理员', 
    'admin@example.com', 
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE
);

-- 插入管理员凭证（密码: Admin@123，BCrypt加密）
INSERT INTO user_credentials (
    user_id, 
    provider, 
    password_hash
) VALUES (
    1, 
    'email', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH'
);

-- 插入普通测试用户（密码: Test@123）
INSERT INTO users (
    username, 
    display_name, 
    email, 
    email_verified,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired
) VALUES (
    'testuser', 
    '测试用户', 
    'test@example.com', 
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE
);

-- 插入测试用户凭证（密码: Test@123）
INSERT INTO user_credentials (
    user_id, 
    provider, 
    password_hash
) VALUES (
    2, 
    'email', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH'
);

-- ============================================
-- 数据库说明
-- ============================================

/*
数据库设计说明：

1. 使用现有的 aio 数据库
2. 主键使用 id 而非 user_id（与原设计保持一致）
3. 支持多种登录方式（email/gitee/github等）
4. 完整的账户状态管理
5. 登录日志审计
6. 密码历史防重复
7. 设备管理
8. 邮箱验证

安全机制：
   1. 登录失败5次锁定15分钟（Redis实现）
   2. 长时间未登录（30天）需要邮箱验证
   3. 密码不能重复使用最近5次
   4. 所有登录行为都有日志记录
   5. 支持管理员手动锁定账户

密码强度要求：
   - 至少8位
   - 必须包含大写字母
   - 必须包含小写字母
   - 必须包含数字
   - 必须包含特殊字符 (@$!%*?&)

连接信息：
   - 主机: 101.42.157.163
   - 数据库: aio
   - 用户: user
   - 密码: 202430904JINGxu
   - 端口: 5432
*/
