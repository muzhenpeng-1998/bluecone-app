-- 商户后台 RBAC 权限管理表
-- 创建时间：2025-12-19
-- 说明：支持商户后台的角色、权限、用户-角色、角色-权限管理，实现细粒度的权限控制

-- ============================================================
-- 1. 后台角色表 bc_admin_role
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_admin_role (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '角色ID（自增主键）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID（租户隔离）',
    
    -- 角色信息
    role_code           VARCHAR(64)     NOT NULL COMMENT '角色编码（如：STORE_ADMIN、STORE_MANAGER、CASHIER）',
    role_name           VARCHAR(128)    NOT NULL COMMENT '角色名称（如：门店管理员、门店经理、收银员）',
    role_type           VARCHAR(32)     NOT NULL DEFAULT 'CUSTOM' COMMENT '角色类型：SYSTEM（系统预置）、CUSTOM（自定义）',
    description         VARCHAR(512)    DEFAULT NULL COMMENT '角色描述',
    
    -- 状态
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT '角色状态：ACTIVE、INACTIVE',
    
    -- 扩展字段
    ext_json            TEXT            DEFAULT NULL COMMENT '扩展信息JSON',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人ID',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人ID',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_role_code (tenant_id, role_code),
    KEY idx_tenant_status (tenant_id, status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台角色表';

-- ============================================================
-- 2. 后台权限表 bc_admin_permission
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_admin_permission (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '权限ID（自增主键）',
    
    -- 权限信息
    permission_code     VARCHAR(128)    NOT NULL COMMENT '权限编码（如：store:view、product:edit、order:manage）',
    permission_name     VARCHAR(128)    NOT NULL COMMENT '权限名称（如：查看门店、编辑商品、管理订单）',
    resource_type       VARCHAR(64)     NOT NULL COMMENT '资源类型（如：STORE、PRODUCT、ORDER、COUPON、WALLET、DASHBOARD）',
    action              VARCHAR(32)     NOT NULL COMMENT '操作类型（如：VIEW、CREATE、EDIT、DELETE、MANAGE）',
    description         VARCHAR(512)    DEFAULT NULL COMMENT '权限描述',
    
    -- 权限层级（用于权限树展示）
    parent_id           BIGINT          DEFAULT NULL COMMENT '父权限ID（用于构建权限树）',
    sort_order          INT             NOT NULL DEFAULT 0 COMMENT '排序值',
    
    -- 状态
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT '权限状态：ACTIVE、INACTIVE',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人ID',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人ID',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_resource_type (resource_type),
    KEY idx_parent_id (parent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台权限表';

-- ============================================================
-- 3. 用户-角色关联表 bc_admin_user_role
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_admin_user_role (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '关联ID（自增主键）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID（租户隔离）',
    user_id             BIGINT          NOT NULL COMMENT '用户ID（关联bc_user.id）',
    role_id             BIGINT          NOT NULL COMMENT '角色ID（关联bc_admin_role.id）',
    
    -- 作用域（可选，用于更细粒度的权限控制）
    scope_type          VARCHAR(32)     DEFAULT NULL COMMENT '作用域类型（如：TENANT、STORE）',
    scope_id            BIGINT          DEFAULT NULL COMMENT '作用域ID（如：门店ID，NULL表示全租户范围）',
    
    -- 有效期（可选）
    valid_from          DATETIME        DEFAULT NULL COMMENT '生效时间',
    valid_until         DATETIME        DEFAULT NULL COMMENT '失效时间',
    
    -- 状态
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT '关联状态：ACTIVE、INACTIVE',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人ID',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人ID',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_user_role_scope (tenant_id, user_id, role_id, scope_type, scope_id),
    KEY idx_tenant_user (tenant_id, user_id, status),
    KEY idx_tenant_role (tenant_id, role_id),
    KEY idx_scope (scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- ============================================================
-- 4. 角色-权限关联表 bc_admin_role_permission
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_admin_role_permission (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '关联ID（自增主键）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID（租户隔离）',
    role_id             BIGINT          NOT NULL COMMENT '角色ID（关联bc_admin_role.id）',
    permission_id       BIGINT          NOT NULL COMMENT '权限ID（关联bc_admin_permission.id）',
    
    -- 状态
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT '关联状态：ACTIVE、INACTIVE',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人ID',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_role_permission (tenant_id, role_id, permission_id),
    KEY idx_tenant_role (tenant_id, role_id),
    KEY idx_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- ============================================================
-- 5. 初始化系统预置权限
-- ============================================================
INSERT INTO bc_admin_permission (permission_code, permission_name, resource_type, action, description, sort_order) VALUES
-- 门店权限
('store:view', '查看门店', 'STORE', 'VIEW', '查看门店基本信息、营业时间等', 100),
('store:edit', '编辑门店', 'STORE', 'EDIT', '修改门店信息、营业时间、地址等', 101),

-- 商品权限
('product:view', '查看商品', 'PRODUCT', 'VIEW', '查看商品列表和详情', 200),
('product:create', '创建商品', 'PRODUCT', 'CREATE', '创建新商品', 201),
('product:edit', '编辑商品', 'PRODUCT', 'EDIT', '修改商品信息、价格等', 202),
('product:online', '商品上下线', 'PRODUCT', 'MANAGE', '商品上线和下线操作', 203),
('product:delete', '删除商品', 'PRODUCT', 'DELETE', '删除商品', 204),

-- 订单权限
('order:view', '查看订单', 'ORDER', 'VIEW', '查看订单列表和详情', 300),
('order:manage', '管理订单', 'ORDER', 'MANAGE', '接单、取消订单等操作', 301),
('order:refund', '订单退款', 'ORDER', 'REFUND', '处理订单退款', 302),

-- 优惠券权限
('coupon:view', '查看优惠券', 'COUPON', 'VIEW', '查看优惠券模板和发放记录', 400),
('coupon:create', '创建优惠券', 'COUPON', 'CREATE', '创建优惠券模板', 401),
('coupon:edit', '编辑优惠券', 'COUPON', 'EDIT', '修改优惠券模板', 402),
('coupon:online', '优惠券上下线', 'COUPON', 'MANAGE', '优惠券模板上线和下线', 403),
('coupon:grant', '发放优惠券', 'COUPON', 'GRANT', '手动发放优惠券给用户', 404),

-- 钱包权限
('wallet:view', '查看钱包', 'WALLET', 'VIEW', '查看用户余额和充值记录', 500),
('wallet:manage', '管理钱包', 'WALLET', 'MANAGE', '钱包余额调整等操作', 501),

-- 仪表盘权限
('dashboard:view', '查看仪表盘', 'DASHBOARD', 'VIEW', '查看经营数据概览', 600),

-- 系统管理权限
('system:role', '角色管理', 'SYSTEM', 'MANAGE', '管理角色和权限', 900),
('system:user', '用户管理', 'SYSTEM', 'MANAGE', '管理后台用户', 901),
('system:audit', '审计日志', 'SYSTEM', 'VIEW', '查看审计日志', 902);

-- ============================================================
-- 6. 初始化系统预置角色
-- ============================================================
-- 注意：这里使用 tenant_id = 0 作为系统角色模板，实际使用时需要为每个租户创建对应的角色
INSERT INTO bc_admin_role (tenant_id, role_code, role_name, role_type, description) VALUES
(0, 'SUPER_ADMIN', '超级管理员', 'SYSTEM', '拥有所有权限的超级管理员'),
(0, 'STORE_ADMIN', '门店管理员', 'SYSTEM', '门店管理员，拥有门店所有功能权限'),
(0, 'STORE_MANAGER', '门店经理', 'SYSTEM', '门店经理，可查看数据和管理订单'),
(0, 'CASHIER', '收银员', 'SYSTEM', '收银员，仅可查看订单和商品');

-- ============================================================
-- 7. 初始化超级管理员角色权限（tenant_id=0 为模板）
-- ============================================================
INSERT INTO bc_admin_role_permission (tenant_id, role_id, permission_id)
SELECT 0, r.id, p.id
FROM bc_admin_role r
CROSS JOIN bc_admin_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND r.tenant_id = 0;

-- ============================================================
-- 8. 初始化门店管理员角色权限（tenant_id=0 为模板）
-- ============================================================
INSERT INTO bc_admin_role_permission (tenant_id, role_id, permission_id)
SELECT 0, r.id, p.id
FROM bc_admin_role r
CROSS JOIN bc_admin_permission p
WHERE r.role_code = 'STORE_ADMIN' AND r.tenant_id = 0
  AND p.permission_code IN (
    'store:view', 'store:edit',
    'product:view', 'product:create', 'product:edit', 'product:online',
    'order:view', 'order:manage', 'order:refund',
    'coupon:view', 'coupon:create', 'coupon:edit', 'coupon:online', 'coupon:grant',
    'wallet:view',
    'dashboard:view'
  );

-- ============================================================
-- 9. 初始化门店经理角色权限（tenant_id=0 为模板）
-- ============================================================
INSERT INTO bc_admin_role_permission (tenant_id, role_id, permission_id)
SELECT 0, r.id, p.id
FROM bc_admin_role r
CROSS JOIN bc_admin_permission p
WHERE r.role_code = 'STORE_MANAGER' AND r.tenant_id = 0
  AND p.permission_code IN (
    'store:view',
    'product:view',
    'order:view', 'order:manage',
    'coupon:view', 'coupon:grant',
    'wallet:view',
    'dashboard:view'
  );

-- ============================================================
-- 10. 初始化收银员角色权限（tenant_id=0 为模板）
-- ============================================================
INSERT INTO bc_admin_role_permission (tenant_id, role_id, permission_id)
SELECT 0, r.id, p.id
FROM bc_admin_role r
CROSS JOIN bc_admin_permission p
WHERE r.role_code = 'CASHIER' AND r.tenant_id = 0
  AND p.permission_code IN (
    'store:view',
    'product:view',
    'order:view'
  );
