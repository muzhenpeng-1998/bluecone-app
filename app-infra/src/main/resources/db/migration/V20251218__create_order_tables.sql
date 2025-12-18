-- 订单模块核心表
-- 创建时间：2025-12-18
-- 说明：订单主链路 M0（确认单 + 提交单 + 落库 + 幂等 + 状态机）

-- ============================================================
-- 订单主表
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_order (
    id                  BIGINT          NOT NULL COMMENT '订单ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    store_id            BIGINT          NOT NULL COMMENT '门店ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    
    -- 订单编号
    order_no            VARCHAR(64)     NOT NULL COMMENT '订单号（对外展示，PublicId格式：ord_xxx）',
    client_order_no     VARCHAR(128)    DEFAULT NULL COMMENT '客户端订单号（用于幂等）',
    
    -- 业务属性
    biz_type            VARCHAR(32)     NOT NULL COMMENT '业务类型：DINE_IN（堂食）、TAKEAWAY（外卖）、PICKUP（自取）',
    order_source        VARCHAR(32)     NOT NULL COMMENT '订单来源：MINI_PROGRAM、H5、POS',
    channel             VARCHAR(32)     DEFAULT NULL COMMENT '渠道标识',
    
    -- 金额字段（单位：元，保留2位小数）
    total_amount        DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '订单总金额（原价总额）',
    discount_amount     DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    payable_amount      DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '应付金额（实际支付金额）',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    
    -- 状态字段
    status              VARCHAR(32)     NOT NULL COMMENT '订单状态：INIT、WAIT_PAY、PAID、WAIT_ACCEPT、ACCEPTED、CANCELED等',
    pay_status          VARCHAR(32)     NOT NULL COMMENT '支付状态：UNPAID、PAID、REFUNDING、REFUNDED',
    
    -- 扩展字段
    order_remark        VARCHAR(512)    DEFAULT NULL COMMENT '订单备注（用户留言）',
    ext_json            TEXT            DEFAULT NULL COMMENT '扩展信息JSON（存储配送信息、联系方式等）',
    
    -- 会话信息（用于协同下单场景）
    session_id          VARCHAR(64)     DEFAULT NULL COMMENT '会话ID（多人协同下单）',
    session_version     INT             DEFAULT 0 COMMENT '会话版本号',
    
    -- 商户接单信息
    accept_operator_id  BIGINT          DEFAULT NULL COMMENT '接单操作人ID',
    accepted_at         DATETIME        DEFAULT NULL COMMENT '接单时间',
    
    -- 用户软删除（用户侧隐藏订单）
    user_deleted        TINYINT(1)      DEFAULT 0 COMMENT '用户是否删除：0=否，1=是',
    user_deleted_at     DATETIME        DEFAULT NULL COMMENT '用户删除时间',
    
    -- 审计字段
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_client_order_no (tenant_id, client_order_no),
    UNIQUE KEY uk_tenant_order_no (tenant_id, order_no),
    KEY idx_tenant_user_status (tenant_id, user_id, status, created_at),
    KEY idx_tenant_store_status (tenant_id, store_id, status, created_at),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- ============================================================
-- 订单明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_order_item (
    id                  BIGINT          NOT NULL COMMENT '明细ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    store_id            BIGINT          NOT NULL COMMENT '门店ID',
    order_id            BIGINT          NOT NULL COMMENT '订单ID（关联bc_order.id）',
    
    -- 商品信息（快照）
    product_id          BIGINT          NOT NULL COMMENT '商品ID',
    sku_id              BIGINT          NOT NULL COMMENT 'SKU ID',
    product_name        VARCHAR(128)    NOT NULL COMMENT '商品名称（快照）',
    sku_name            VARCHAR(128)    DEFAULT NULL COMMENT 'SKU名称（快照）',
    product_code        VARCHAR(64)     DEFAULT NULL COMMENT '商品编码（快照）',
    
    -- 数量与金额
    quantity            INT             NOT NULL DEFAULT 1 COMMENT '购买数量',
    unit_price          DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '单价（快照）',
    discount_amount     DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    payable_amount      DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '应付金额（单价*数量-优惠）',
    
    -- 扩展字段
    attrs_json          TEXT            DEFAULT NULL COMMENT '商品属性JSON（规格、口味等）',
    remark              VARCHAR(512)    DEFAULT NULL COMMENT '明细备注',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人',
    
    PRIMARY KEY (id),
    KEY idx_tenant_order (tenant_id, order_id),
    KEY idx_tenant_product (tenant_id, product_id),
    KEY idx_tenant_sku (tenant_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- ============================================================
-- 幂等记录表（如果不存在则创建）
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_idempotency_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    biz_type        VARCHAR(64)     NOT NULL COMMENT '业务类型：ORDER_SUBMIT、ORDER_CONFIRM等',
    idem_key        VARCHAR(128)    NOT NULL COMMENT '幂等键（业务生成）',
    request_hash    VARCHAR(64)     NOT NULL COMMENT '请求摘要（SHA-256）',
    
    -- 状态与结果
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0=PROCESSING，1=SUCCEEDED，2=FAILED',
    result_ref      VARCHAR(255)    DEFAULT NULL COMMENT '结果引用（如订单号、PublicId等）',
    result_json     TEXT            DEFAULT NULL COMMENT '结果JSON（完整返回值）',
    
    -- 错误信息
    error_code      VARCHAR(64)     DEFAULT NULL COMMENT '错误码',
    error_msg       VARCHAR(512)    DEFAULT NULL COMMENT '错误消息',
    
    -- 过期与锁
    expire_at       DATETIME        NOT NULL COMMENT '记录过期时间',
    lock_until      DATETIME        NOT NULL COMMENT '租约锁定时间',
    
    -- 审计字段
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_biztype_idemkey (tenant_id, biz_type, idem_key),
    KEY idx_tenant_status_expire (tenant_id, status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='幂等记录表';

-- ============================================================
-- PublicId映射表（如果不存在则创建）
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_public_id_map (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    resource_type   VARCHAR(32)     NOT NULL COMMENT '资源类型：ORDER、STORE、PRODUCT等',
    public_id       VARCHAR(64)     NOT NULL COMMENT '对外公开ID（如：ord_xxx）',
    internal_id     BIGINT          NOT NULL COMMENT '内部ID（ULID）',
    
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_resource_public (tenant_id, resource_type, public_id),
    UNIQUE KEY uk_tenant_resource_internal (tenant_id, resource_type, internal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PublicId映射表';
