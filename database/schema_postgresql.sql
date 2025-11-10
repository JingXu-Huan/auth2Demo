-- ============================================
-- OAuth2 认证系统 PostgreSQL 数据库建表语句
-- 版本: 2.0
-- 日期: 2025-11-10
-- 作者: Junjie
-- 数据库: PostgreSQL
-- ============================================

-- 删除已存在的表（按依赖关系倒序删除）
DROP TABLE IF EXISTS login_logs CASCADE;
DROP TABLE IF EXISTS password_history CASCADE;
DROP TABLE IF EXISTS user_credentials CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================
-- 1. 用户主表
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    email_verified BOOLEAN DEFAULT FALSE,
    avatar_url VARCHAR(500),
    
    -- 邮箱验证相关
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

-- 创建索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_users_last_login ON users(last_login_at);

-- 创建更新时间触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 添加表注释
COMMENT ON TABLE users IS '用户主表';
COMMENT ON COLUMN users.id IS '用户ID';
COMMENT ON COLUMN users.username IS '用户名';
COMMENT ON COLUMN users.display_name IS '显示名称';
COMMENT ON COLUMN users.email IS '邮箱';
COMMENT ON COLUMN users.email_verified IS '邮箱是否已验证';
COMMENT ON COLUMN users.enabled IS '账户是否启用';
COMMENT ON COLUMN users.account_non_expired IS '账户是否未过期';
COMMENT ON COLUMN users.account_non_locked IS '账户是否未锁定（管理员手动锁定）';
COMMENT ON COLUMN users.credentials_non_expired IS '密码是否未过期';
COMMENT ON COLUMN users.lock_reason IS '账户锁定原因';
COMMENT ON COLUMN users.last_login_ip IS '最后登录IP';

-- ============================================
-- 2. 用户凭证表（支持多种登录方式）
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

-- 创建索引
CREATE INDEX idx_user_credentials_provider_user ON user_credentials(provider, provider_user_id);

-- 创建更新时间触发器
CREATE TRIGGER update_user_credentials_updated_at BEFORE UPDATE ON user_credentials
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 添加表注释
COMMENT ON TABLE user_credentials IS '用户凭证表';
COMMENT ON COLUMN user_credentials.provider IS '认证提供商（email/gitee/github等）';
COMMENT ON COLUMN user_credentials.provider_user_id IS '第三方平台用户ID';
COMMENT ON COLUMN user_credentials.password_hash IS '密码哈希（仅email登录）';

-- ============================================
-- 3. 密码历史表（防止重复使用旧密码）
-- ============================================
CREATE TABLE password_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_password_history_user_id ON password_history(user_id);
CREATE INDEX idx_password_history_created_at ON password_history(created_at);

-- 添加表注释
COMMENT ON TABLE password_history IS '密码历史表';
COMMENT ON COLUMN password_history.user_id IS '用户ID';
COMMENT ON COLUMN password_history.password_hash IS '密码哈希';

-- ============================================
-- 4. 登录日志表（审计和安全分析）
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

-- 创建索引
CREATE INDEX idx_login_logs_user_id ON login_logs(user_id);
CREATE INDEX idx_login_logs_email ON login_logs(email);
CREATE INDEX idx_login_logs_ip_address ON login_logs(ip_address);
CREATE INDEX idx_login_logs_login_status ON login_logs(login_status);
CREATE INDEX idx_login_logs_created_at ON login_logs(created_at);

-- 添加表注释
COMMENT ON TABLE login_logs IS '登录日志表';
COMMENT ON COLUMN login_logs.login_status IS '登录状态（SUCCESS/FAILED/BLOCKED）';
COMMENT ON COLUMN login_logs.failure_reason IS '失败原因';
COMMENT ON COLUMN login_logs.location IS '登录地点（可选）';
COMMENT ON COLUMN login_logs.device_type IS '设备类型（PC/Mobile/Tablet）';

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
-- 注意：需要使用实际的BCrypt哈希值
INSERT INTO user_credentials (
    user_id, 
    provider, 
    password_hash
) VALUES (
    1, 
    'email', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH'  -- 需要替换为实际的BCrypt哈希
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
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH'  -- 需要替换为实际的BCrypt哈希
);

-- ============================================
-- 数据库说明
-- ============================================

/*
PostgreSQL 特性说明：

1. 自增主键：
   - MySQL: BIGINT AUTO_INCREMENT
   - PostgreSQL: BIGSERIAL (等同于 BIGINT + SEQUENCE)

2. 布尔类型：
   - MySQL: BOOLEAN (实际是TINYINT)
   - PostgreSQL: BOOLEAN (真正的布尔类型)

3. 自动更新时间戳：
   - MySQL: ON UPDATE CURRENT_TIMESTAMP
   - PostgreSQL: 使用触发器 (TRIGGER + FUNCTION)

4. 外键约束：
   - PostgreSQL 完全支持外键约束
   - 使用 ON DELETE CASCADE 和 ON DELETE SET NULL

5. 索引：
   - PostgreSQL 索引性能优秀
   - 支持部分索引、表达式索引等高级特性

6. 字符集：
   - PostgreSQL 默认使用 UTF-8
   - 不需要像 MySQL 那样指定 CHARSET 和 COLLATE

密码强度要求：
   - 至少8位
   - 必须包含大写字母
   - 必须包含小写字母
   - 必须包含数字
   - 必须包含特殊字符 (@$!%*?&)

安全机制：
   1. 登录失败5次锁定15分钟（Redis实现）
   2. 长时间未登录（30天）需要邮箱验证
   3. 密码不能重复使用最近5次
   4. 所有登录行为都有日志记录
   5. 支持管理员手动锁定账户

连接信息：
   - 主机: 101.42.157.163
   - 数据库: aio
   - 用户: user
   - 密码: 202430904JINGxu
   - 端口: 5432 (PostgreSQL默认端口)
*/
