-- ========================================================================
-- 组织架构与权限系统 (Organization & Permission Database) - RBAC + ReBAC
-- ========================================================================

CREATE DATABASE org_db;
\c org_db;

-- 6.1 组织表（多租户）
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    org_code VARCHAR(50) UNIQUE NOT NULL, -- 组织代码
    name VARCHAR(100) NOT NULL,
    full_name VARCHAR(200),
    
    -- 组织类型
    org_type VARCHAR(50), -- company, school, government, ngo
    industry VARCHAR(50),
    
    -- 规模与配置
    employee_count INT DEFAULT 0,
    max_members INT DEFAULT 1000,
    
    -- 联系信息
    contact_email VARCHAR(100),
    contact_phone VARCHAR(50),
    website VARCHAR(200),
    
    -- 地址
    country VARCHAR(50),
    province VARCHAR(50),
    city VARCHAR(50),
    address TEXT,
    
    -- 认证信息
    is_verified BOOLEAN DEFAULT FALSE, -- 是否认证
    verified_at TIMESTAMPTZ,
    business_license VARCHAR(100), -- 营业执照号
    
    -- 功能配置
    features JSONB DEFAULT '{}', -- 功能开关
    settings JSONB DEFAULT '{}', -- 组织设置
    
    -- Logo
    logo_url VARCHAR(500),
    
    status SMALLINT DEFAULT 1, -- 1:正常, 2:冻结, 3:注销
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_organizations_status ON organizations(status);
CREATE INDEX idx_organizations_code ON organizations(org_code);

-- 6.2 部门表 (树形结构)
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    parent_id BIGINT REFERENCES departments(id) ON DELETE CASCADE,
    
    -- 部门信息
    dept_code VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    full_name VARCHAR(500), -- 完整名称
    
    -- 树形结构
    path VARCHAR(1000), -- 部门路径: /1/2/3
    path_names VARCHAR(2000), -- 路径名称: 总部/研发部/后端组
    level INT NOT NULL DEFAULT 1,
    sort_order INT DEFAULT 0,
    
    -- 部门负责人
    manager_id BIGINT,
    deputy_ids BIGINT[], -- 副职领导
    
    -- 统计
    member_count INT DEFAULT 0,
    sub_dept_count INT DEFAULT 0,
    
    -- 配置
    budget DECIMAL(15,2), -- 预算
    cost_center VARCHAR(50), -- 成本中心
    
    status SMALLINT DEFAULT 1, -- 1:正常, 2:撤销
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_departments_org ON departments(org_id);
CREATE INDEX idx_departments_parent ON departments(parent_id);
CREATE INDEX idx_departments_path ON departments(path);
CREATE INDEX idx_departments_manager ON departments(manager_id);

-- 6.3 组织成员表
CREATE TABLE org_members (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    user_id BIGINT NOT NULL,
    dept_id BIGINT REFERENCES departments(id),
    
    -- 员工信息
    employee_no VARCHAR(50),
    work_email VARCHAR(100),
    work_phone VARCHAR(50),
    
    -- 职位信息
    job_title VARCHAR(100), -- 职位
    job_level VARCHAR(50), -- 职级
    job_sequence VARCHAR(50), -- 序列（技术/管理/专业）
    
    -- 入职信息
    hire_date DATE,
    probation_end_date DATE, -- 试用期结束
    contract_end_date DATE, -- 合同结束
    
    -- 汇报关系
    direct_manager_id BIGINT, -- 直属上级
    dotted_manager_id BIGINT, -- 虚线上级
    
    -- 工作地点
    office_location VARCHAR(100),
    seat_number VARCHAR(50),
    
    -- 状态
    status SMALLINT DEFAULT 1, -- 1:在职, 2:离职, 3:休假
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ -- 离职时间
);

CREATE UNIQUE INDEX idx_org_members_unique ON org_members(org_id, user_id) WHERE status = 1;
CREATE INDEX idx_org_members_dept ON org_members(dept_id);
CREATE INDEX idx_org_members_employee_no ON org_members(employee_no);
CREATE INDEX idx_org_members_manager ON org_members(direct_manager_id);

-- 6.4 角色表
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT REFERENCES organizations(id), -- NULL表示系统角色
    
    -- 角色信息
    role_code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    
    -- 角色类型
    role_type SMALLINT DEFAULT 1, -- 1:自定义, 2:系统预设, 3:部门角色
    
    -- 角色级别（用于权限继承）
    role_level INT DEFAULT 100,
    parent_role_id BIGINT REFERENCES roles(id), -- 父角色
    
    -- 配置
    is_default BOOLEAN DEFAULT FALSE,
    max_members INT, -- 最大成员数
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(COALESCE(org_id, 0), role_code)
);

CREATE INDEX idx_roles_org ON roles(org_id);
CREATE INDEX idx_roles_parent ON roles(parent_role_id);

-- 6.5 权限表
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    
    -- 权限标识
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    
    -- 权限信息
    name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- 权限分类
    module VARCHAR(50) NOT NULL, -- 模块：im, doc, file, admin
    resource VARCHAR(100) NOT NULL, -- 资源：message, channel, document
    action VARCHAR(50) NOT NULL, -- 操作：create, read, update, delete
    
    -- 权限级别
    risk_level SMALLINT DEFAULT 1, -- 1:低风险, 2:中风险, 3:高风险
    
    -- 依赖权限
    depends_on BIGINT[], -- 依赖的其他权限ID
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_permissions_module ON permissions(module);
CREATE INDEX idx_permissions_resource ON permissions(resource);

-- 预置核心权限
INSERT INTO permissions (permission_code, name, module, resource, action, risk_level) VALUES
    -- IM权限
    ('im.message.send', '发送消息', 'im', 'message', 'send', 1),
    ('im.message.recall', '撤回消息', 'im', 'message', 'recall', 2),
    ('im.message.delete', '删除消息', 'im', 'message', 'delete', 3),
    ('im.channel.create', '创建群组', 'im', 'channel', 'create', 1),
    ('im.channel.manage', '管理群组', 'im', 'channel', 'manage', 2),
    ('im.channel.dissolve', '解散群组', 'im', 'channel', 'dissolve', 3),
    
    -- 文档权限
    ('doc.document.create', '创建文档', 'doc', 'document', 'create', 1),
    ('doc.document.edit', '编辑文档', 'doc', 'document', 'edit', 1),
    ('doc.document.delete', '删除文档', 'doc', 'document', 'delete', 2),
    ('doc.document.share', '分享文档', 'doc', 'document', 'share', 2),
    
    -- 文件权限
    ('file.upload', '上传文件', 'file', 'file', 'upload', 1),
    ('file.download', '下载文件', 'file', 'file', 'download', 1),
    ('file.delete', '删除文件', 'file', 'file', 'delete', 2),
    
    -- 管理权限
    ('admin.user.manage', '用户管理', 'admin', 'user', 'manage', 3),
    ('admin.role.manage', '角色管理', 'admin', 'role', 'manage', 3),
    ('admin.org.manage', '组织管理', 'admin', 'org', 'manage', 3);

-- 6.6 角色权限关联表
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    
    -- 授权信息
    granted_by BIGINT,
    granted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- 6.7 用户角色关联表
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    
    -- 授权范围
    scope_type VARCHAR(20) DEFAULT 'org', -- global, org, dept, team
    scope_id BIGINT, -- 组织/部门/团队ID
    
    -- 有效期
    effective_date DATE,
    expiry_date DATE,
    
    -- 授权信息
    granted_by BIGINT,
    granted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    
    UNIQUE(user_id, role_id, scope_type, COALESCE(scope_id, 0))
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_user_roles_role ON user_roles(role_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_user_roles_scope ON user_roles(scope_type, scope_id);

-- 6.8 权限策略表（细粒度权限控制）
CREATE TABLE permission_policies (
    id BIGSERIAL PRIMARY KEY,
    
    name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- 策略条件（使用类似AWS IAM的策略语言）
    effect VARCHAR(10) NOT NULL, -- allow, deny
    principals JSONB, -- {"users": [], "roles": [], "departments": []}
    resources JSONB, -- {"types": ["document"], "ids": [], "tags": []}
    actions TEXT[], -- ["read", "write", "delete"]
    conditions JSONB, -- {"ip_range": "10.0.0.0/8", "time_range": {...}}
    
    -- 优先级（数字越小优先级越高）
    priority INT DEFAULT 100,
    
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_permission_policies_enabled ON permission_policies(enabled);
CREATE INDEX idx_permission_policies_priority ON permission_policies(priority);

-- 6.9 数据权限表（行级权限）
CREATE TABLE data_permissions (
    id BIGSERIAL PRIMARY KEY,
    
    -- 权限主体
    principal_type VARCHAR(20) NOT NULL, -- user, role, dept
    principal_id BIGINT NOT NULL,
    
    -- 数据范围
    resource_type VARCHAR(50) NOT NULL, -- 资源类型
    
    -- 范围定义
    scope_type VARCHAR(20) NOT NULL, -- all, org, dept, self, custom
    scope_value JSONB, -- 自定义范围条件
    
    -- 操作权限
    can_read BOOLEAN DEFAULT TRUE,
    can_write BOOLEAN DEFAULT FALSE,
    can_delete BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(principal_type, principal_id, resource_type)
);

CREATE INDEX idx_data_permissions_principal ON data_permissions(principal_type, principal_id);
CREATE INDEX idx_data_permissions_resource ON data_permissions(resource_type);

-- 6.10 操作审计日志表
CREATE TABLE audit_logs (
    id BIGSERIAL,
    
    -- 操作者
    user_id BIGINT NOT NULL,
    org_id BIGINT,
    
    -- 操作信息
    action VARCHAR(100) NOT NULL, -- 操作类型
    resource_type VARCHAR(50), -- 资源类型
    resource_id VARCHAR(100), -- 资源ID
    
    -- 操作详情
    details JSONB,
    changes JSONB, -- 变更内容 {"before": {}, "after": {}}
    
    -- 结果
    success BOOLEAN NOT NULL,
    error_message TEXT,
    
    -- 环境信息
    ip_address INET,
    user_agent TEXT,
    request_id VARCHAR(100),
    
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (created_at, id)
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE audit_logs_2024_11 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

-- 6.11 权限缓存表（提高查询性能）
CREATE TABLE permission_cache (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- 缓存的权限集合
    permissions TEXT[], -- 权限代码数组
    roles BIGINT[], -- 角色ID数组
    
    -- 数据权限范围
    data_scopes JSONB,
    
    -- 缓存有效期
    calculated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ DEFAULT (CURRENT_TIMESTAMP + INTERVAL '1 hour'),
    
    UNIQUE(user_id)
);

CREATE INDEX idx_permission_cache_expires ON permission_cache(expires_at);

-- 函数：获取用户所有权限（包括角色继承）
CREATE OR REPLACE FUNCTION get_user_permissions(p_user_id BIGINT)
RETURNS TEXT[] AS $$
DECLARE
    v_permissions TEXT[];
BEGIN
    WITH RECURSIVE role_hierarchy AS (
        -- 用户直接拥有的角色
        SELECT r.id, r.parent_role_id
        FROM roles r
        JOIN user_roles ur ON r.id = ur.role_id
        WHERE ur.user_id = p_user_id AND ur.revoked_at IS NULL
        
        UNION
        
        -- 递归获取父角色
        SELECT r.id, r.parent_role_id
        FROM roles r
        JOIN role_hierarchy rh ON r.id = rh.parent_role_id
    )
    SELECT ARRAY_AGG(DISTINCT p.permission_code)
    INTO v_permissions
    FROM role_hierarchy rh
    JOIN role_permissions rp ON rh.id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.id;
    
    RETURN COALESCE(v_permissions, ARRAY[]::TEXT[]);
END;
$$ LANGUAGE plpgsql;
