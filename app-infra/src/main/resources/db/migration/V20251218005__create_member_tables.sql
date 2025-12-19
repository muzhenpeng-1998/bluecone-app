-- 会员与积分账本 M5：会员表 + 积分账户 + 积分流水
-- 创建时间：2025-12-18
-- 说明：支持多租户会员体系，积分账本采用幂等设计，保证资产安全

-- ============================================================
-- 1. 会员表 bc_member
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_member (
    id                  BIGINT          NOT NULL COMMENT '会员ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    user_id             BIGINT          NOT NULL COMMENT '平台用户ID（关联 bc_user.id，占位）',
    
    -- 会员编号（租户内唯一，对外展示）
    member_no           VARCHAR(64)     NOT NULL COMMENT '会员号（租户内唯一，如 mb_001, mb_002）',
    
    -- 会员状态
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT '会员状态：ACTIVE=正常、INACTIVE=停用、FROZEN=冻结',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_user (tenant_id, user_id) COMMENT '租户+用户唯一约束（一个用户在一个租户只能有一个会员）',
    UNIQUE KEY uk_tenant_member_no (tenant_id, member_no) COMMENT '租户+会员号唯一约束',
    KEY idx_tenant_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员表';

-- ============================================================
-- 2. 积分账户表 bc_points_account
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_points_account (
    id                  BIGINT          NOT NULL COMMENT '账户ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    member_id           BIGINT          NOT NULL COMMENT '会员ID（关联 bc_member.id）',
    
    -- 积分余额
    available_points    BIGINT          NOT NULL DEFAULT 0 COMMENT '可用积分',
    frozen_points       BIGINT          NOT NULL DEFAULT 0 COMMENT '冻结积分（下单锁定，等待支付完成或取消）',
    
    -- 乐观锁
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（每次更新账户余额时递增）',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_member (tenant_id, member_id) COMMENT '租户+会员唯一约束（一个会员只有一个积分账户）',
    KEY idx_tenant_available (tenant_id, available_points)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分账户表';

-- ============================================================
-- 3. 积分流水表 bc_points_ledger（账本核心）
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_points_ledger (
    id                  BIGINT          NOT NULL COMMENT '流水ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    member_id           BIGINT          NOT NULL COMMENT '会员ID（关联 bc_member.id）',
    
    -- 积分变动方向与金额
    direction           VARCHAR(32)     NOT NULL COMMENT '变动方向：EARN=获得、SPEND=消费、FREEZE=冻结、RELEASE=释放、REVERT=回退、ADJUST=调整',
    delta_points        BIGINT          NOT NULL COMMENT '变动积分值（正数，具体加减由 direction 决定）',
    
    -- 变动前后余额（快照，便于对账）
    before_available    BIGINT          NOT NULL DEFAULT 0 COMMENT '变动前可用积分',
    before_frozen       BIGINT          NOT NULL DEFAULT 0 COMMENT '变动前冻结积分',
    after_available     BIGINT          NOT NULL DEFAULT 0 COMMENT '变动后可用积分',
    after_frozen        BIGINT          NOT NULL DEFAULT 0 COMMENT '变动后冻结积分',
    
    -- 业务关联
    biz_type            VARCHAR(64)     NOT NULL COMMENT '业务类型：ORDER_PAY、ORDER_COMPLETE、REFUND、ADJUST 等',
    biz_id              VARCHAR(128)    NOT NULL COMMENT '业务ID（订单ID、退款单ID等）',
    
    -- 幂等键（全局唯一，防止重复操作）
    idempotency_key     VARCHAR(256)    NOT NULL COMMENT '幂等键（格式：{tenantId}:{bizType}:{bizId}:{operation}）',
    
    -- 备注说明
    remark              VARCHAR(512)    DEFAULT NULL COMMENT '备注说明（可选）',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_idem (tenant_id, idempotency_key) COMMENT '租户+幂等键唯一约束（保证同一操作只执行一次）',
    KEY idx_tenant_member_time (tenant_id, member_id, created_at) COMMENT '会员积分流水查询索引',
    KEY idx_tenant_biz (tenant_id, biz_type, biz_id) COMMENT '业务流水查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水表（账本）';

-- ============================================================
-- 4. 初始化说明
-- ============================================================
-- 说明：
-- 1. bc_member：会员主表，tenant_id + user_id 唯一，保证一个用户在一个租户只有一个会员
-- 2. bc_points_account：积分账户表，tenant_id + member_id 唯一，采用乐观锁 version 保证并发安全
-- 3. bc_points_ledger：积分流水表，所有积分变动都必须记录流水，幂等键 idempotency_key 保证同一操作只执行一次
-- 4. 积分操作流程：
--    - 冻结：available_points - delta_points, frozen_points + delta_points（方向 FREEZE）
--    - 提交扣减：frozen_points - delta_points（方向 SPEND，如果有冻结）或 available_points - delta_points（无冻结）
--    - 提交入账：available_points + delta_points（方向 EARN）
--    - 释放：frozen_points - delta_points, available_points + delta_points（方向 RELEASE）
--    - 回退：available_points + delta_points（方向 REVERT）
-- 5. 幂等键规则示例：
--    - 订单完成赚取积分：{tenantId}:ORDER_COMPLETE:{orderId}:earn
--    - 退款返还积分：{tenantId}:REFUND:{refundId}:revert
--    - 下单冻结积分：{tenantId}:ORDER_PAY:{orderId}:freeze
