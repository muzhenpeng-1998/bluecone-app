-- 储值钱包 M1：账户 + 账本 + 冻结 + 充值单
-- 创建时间：2025-12-18
-- 说明：支持余额账户、资金流水账本化、冻结/提交/释放/回退，所有操作幂等

-- ============================================================
-- 1. 钱包账户表 bc_wallet_account
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_wallet_account (
    id                  BIGINT          NOT NULL COMMENT '账户ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    
    -- 账户余额（单位：元，保留2位小数）
    available_balance   DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    frozen_balance      DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '冻结余额（下单锁定）',
    total_recharged     DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '累计充值金额（统计字段）',
    total_consumed      DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '累计消费金额（统计字段）',
    
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    
    -- 账户状态
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态：ACTIVE、FROZEN、CLOSED',
    
    -- 乐观锁（并发控制）
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（每次余额变更+1）',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_user (tenant_id, user_id),
    KEY idx_tenant_status (tenant_id, status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包账户表';

-- ============================================================
-- 2. 钱包账本表 bc_wallet_ledger
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_wallet_ledger (
    id                  BIGINT          NOT NULL COMMENT '流水ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    account_id          BIGINT          NOT NULL COMMENT '账户ID（关联bc_wallet_account.id）',
    
    -- 流水编号
    ledger_no           VARCHAR(64)     NOT NULL COMMENT '流水号（对外展示，PublicId格式：wl_xxx）',
    
    -- 交易类型
    biz_type            VARCHAR(32)     NOT NULL COMMENT '业务类型：RECHARGE、ORDER_PAY、REFUND、ADJUST',
    biz_order_id        BIGINT          DEFAULT NULL COMMENT '关联业务单ID（订单ID、充值单ID等）',
    biz_order_no        VARCHAR(64)     DEFAULT NULL COMMENT '关联业务单号（冗余，方便查询）',
    
    -- 金额变更（单位：元，保留2位小数）
    amount              DECIMAL(18,2)   NOT NULL COMMENT '变更金额（正数=入账，负数=出账）',
    balance_before      DECIMAL(18,2)   NOT NULL COMMENT '变更前可用余额',
    balance_after       DECIMAL(18,2)   NOT NULL COMMENT '变更后可用余额',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    
    -- 流水描述
    remark              VARCHAR(256)    DEFAULT NULL COMMENT '流水备注',
    
    -- 幂等键（唯一，保证同一请求只生成一条流水）
    idem_key            VARCHAR(128)    NOT NULL COMMENT '幂等键（格式：{tenantId}:{userId}:{bizType}:{bizOrderId}:{operationType}）',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_idem_key (tenant_id, idem_key),
    UNIQUE KEY uk_tenant_ledger_no (tenant_id, ledger_no),
    KEY idx_tenant_user_created (tenant_id, user_id, created_at DESC),
    KEY idx_tenant_account (tenant_id, account_id, created_at DESC),
    KEY idx_biz_order (tenant_id, biz_type, biz_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包账本表';

-- ============================================================
-- 3. 钱包冻结记录表 bc_wallet_freeze
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_wallet_freeze (
    id                  BIGINT          NOT NULL COMMENT '冻结ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    account_id          BIGINT          NOT NULL COMMENT '账户ID（关联bc_wallet_account.id）',
    
    -- 冻结编号
    freeze_no           VARCHAR(64)     NOT NULL COMMENT '冻结单号（对外展示，PublicId格式：wfz_xxx）',
    
    -- 业务关联
    biz_type            VARCHAR(32)     NOT NULL COMMENT '业务类型：ORDER_CHECKOUT（下单冻结）',
    biz_order_id        BIGINT          NOT NULL COMMENT '关联业务单ID（订单ID）',
    biz_order_no        VARCHAR(64)     DEFAULT NULL COMMENT '关联业务单号（冗余）',
    
    -- 冻结金额（单位：元，保留2位小数）
    frozen_amount       DECIMAL(18,2)   NOT NULL COMMENT '冻结金额',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    
    -- 冻结状态
    status              VARCHAR(32)     NOT NULL DEFAULT 'FROZEN' COMMENT '冻结状态：FROZEN、COMMITTED、RELEASED、REVERTED',
    
    -- 幂等键（唯一，保证同一请求只冻结一次）
    idem_key            VARCHAR(128)    NOT NULL COMMENT '幂等键（格式：{tenantId}:{userId}:{bizOrderId}:freeze）',
    
    -- 冻结时间
    frozen_at           DATETIME        NOT NULL COMMENT '冻结时间',
    expires_at          DATETIME        DEFAULT NULL COMMENT '过期时间（超时自动释放）',
    
    -- 提交/释放/回退时间
    committed_at        DATETIME        DEFAULT NULL COMMENT '提交时间（支付成功）',
    released_at         DATETIME        DEFAULT NULL COMMENT '释放时间（取消/超时）',
    reverted_at         DATETIME        DEFAULT NULL COMMENT '回退时间（退款）',
    
    -- 审计字段
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_idem_key (tenant_id, idem_key),
    UNIQUE KEY uk_tenant_freeze_no (tenant_id, freeze_no),
    KEY idx_tenant_user_status (tenant_id, user_id, status, frozen_at DESC),
    KEY idx_tenant_biz_order (tenant_id, biz_type, biz_order_id),
    KEY idx_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包冻结记录表';

-- ============================================================
-- 4. 充值单表 bc_wallet_recharge_order
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_wallet_recharge_order (
    id                  BIGINT          NOT NULL COMMENT '充值单ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    account_id          BIGINT          NOT NULL COMMENT '账户ID（关联bc_wallet_account.id）',
    
    -- 充值单编号
    recharge_id         VARCHAR(64)     NOT NULL COMMENT '充值单号（对外展示，PublicId格式：wrc_xxx）',
    
    -- 充值金额（单位：元，保留2位小数）
    recharge_amount     DECIMAL(18,2)   NOT NULL COMMENT '充值金额',
    bonus_amount        DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '赠送金额（充值活动赠送）',
    total_amount        DECIMAL(18,2)   NOT NULL COMMENT '总到账金额（recharge_amount + bonus_amount）',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    
    -- 充值状态
    status              VARCHAR(32)     NOT NULL DEFAULT 'INIT' COMMENT '充值状态：INIT、PAYING、SUCCESS、FAILED、CLOSED',
    
    -- 支付信息
    pay_order_id        BIGINT          DEFAULT NULL COMMENT '支付单ID（关联bc_payment_order.id）',
    pay_channel         VARCHAR(32)     DEFAULT NULL COMMENT '支付渠道：WECHAT、ALIPAY',
    pay_no              VARCHAR(128)    DEFAULT NULL COMMENT '第三方支付单号',
    
    -- 充值时间
    recharge_requested_at DATETIME      NOT NULL COMMENT '充值发起时间',
    recharge_completed_at DATETIME      DEFAULT NULL COMMENT '充值完成时间（SUCCESS时填充）',
    
    -- 幂等键（唯一，保证同一请求只创建一个充值单）
    idem_key            VARCHAR(128)    NOT NULL COMMENT '幂等键（格式：{tenantId}:{userId}:recharge:{requestId}）',
    
    -- 扩展字段
    ext_json            TEXT            DEFAULT NULL COMMENT '扩展信息JSON',
    
    -- 审计字段
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_idem_key (tenant_id, idem_key),
    UNIQUE KEY uk_tenant_recharge_id (tenant_id, recharge_id),
    KEY idx_tenant_user_status (tenant_id, user_id, status, recharge_requested_at DESC),
    KEY idx_tenant_account (tenant_id, account_id),
    KEY idx_status_requested (status, recharge_requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值单表';

-- ============================================================
-- 5. 索引说明
-- ============================================================
-- bc_wallet_account:
--   - uk_tenant_user: 保证一个用户只有一个钱包账户
--   - idx_tenant_status: 查询活跃账户
--   - idx_created_at: 统计新增账户

-- bc_wallet_ledger:
--   - uk_tenant_idem_key: 幂等键唯一约束，防重
--   - uk_tenant_ledger_no: 流水号唯一约束
--   - idx_tenant_user_created: 用户流水查询（降序）
--   - idx_tenant_account: 账户流水查询
--   - idx_biz_order: 业务单关联查询

-- bc_wallet_freeze:
--   - uk_tenant_idem_key: 幂等键唯一约束，防重
--   - uk_tenant_freeze_no: 冻结单号唯一约束
--   - idx_tenant_user_status: 用户冻结记录查询
--   - idx_tenant_biz_order: 业务单关联查询
--   - idx_status_expires: 超时释放扫描

-- bc_wallet_recharge_order:
--   - uk_tenant_idem_key: 幂等键唯一约束，防重
--   - uk_tenant_recharge_id: 充值单号唯一约束
--   - idx_tenant_user_status: 用户充值记录查询
--   - idx_tenant_account: 账户充值查询
--   - idx_status_requested: 充值单状态查询
