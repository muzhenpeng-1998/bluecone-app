-- ===================================================================
-- Migration: V20251219011__create_campaign_tables.sql
-- Description: 活动编排系统：活动配置、执行日志表
-- Author: System
-- Date: 2025-12-19
-- ===================================================================

-- 活动配置表
CREATE TABLE IF NOT EXISTS bc_campaign (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '活动ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    campaign_code VARCHAR(64) NOT NULL COMMENT '活动编码（租户内唯一）',
    campaign_name VARCHAR(128) NOT NULL COMMENT '活动名称',
    campaign_type VARCHAR(32) NOT NULL COMMENT '活动类型：ORDER_DISCOUNT-订单满减, ORDER_REBATE_COUPON-订单返券, RECHARGE_BONUS-充值赠送',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态：DRAFT-草稿, ONLINE-已上线, OFFLINE-已下线, EXPIRED-已过期',
    rules_json TEXT NOT NULL COMMENT '活动规则（JSON格式）
        通用规则字段：
        - minAmount: 最低金额门槛（BigDecimal）
        - firstOrderOnly: 是否限首单（Boolean）
        - perUserLimit: 每用户参与次数限制（Integer, null表示不限）
        
        ORDER_DISCOUNT 专用规则：
        - discountAmount: 满减金额（BigDecimal）
        - discountRate: 折扣率（BigDecimal, 如0.85表示85折）
        - maxDiscountAmount: 最高优惠封顶（BigDecimal）
        
        ORDER_REBATE_COUPON 专用规则：
        - couponTemplateIds: 券模板ID列表，逗号分隔（String）
        - couponQuantity: 每个模板发放数量（Integer, 默认1）
        
        RECHARGE_BONUS 专用规则：
        - bonusAmount: 固定赠送金额（BigDecimal）
        - bonusRate: 赠送比例（BigDecimal, 如0.1表示赠送10%）
        - maxBonusAmount: 最高赠送封顶（BigDecimal）',
    scope_json TEXT NOT NULL COMMENT '活动适用范围（JSON格式）
        字段说明：
        - scopeType: 范围类型（ALL-全部, STORE-指定门店, CHANNEL-指定渠道）
        - storeIds: 门店ID列表（Array<Long>, scopeType=STORE时使用）
        - channels: 渠道列表（Array<String>, scopeType=CHANNEL时使用）',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '活动结束时间（NULL表示长期有效）',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级（数字越大优先级越高，用于多活动排序）',
    description TEXT DEFAULT NULL COMMENT '活动描述',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_tenant_code (tenant_id, campaign_code),
    INDEX idx_tenant_type_status (tenant_id, campaign_type, status),
    INDEX idx_time_range (start_time, end_time),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动配置表';

-- 活动执行日志表
CREATE TABLE IF NOT EXISTS bc_campaign_execution_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '日志ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    campaign_id BIGINT NOT NULL COMMENT '活动ID',
    campaign_code VARCHAR(64) NOT NULL COMMENT '活动编码',
    campaign_type VARCHAR(32) NOT NULL COMMENT '活动类型',
    idempotency_key VARCHAR(256) NOT NULL COMMENT '幂等键（全局唯一）
        格式：{tenantId}:{campaignType}:{bizOrderId}:{userId}
        说明：保证同一用户在同一业务单上对同一类型活动只执行一次',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    biz_order_id BIGINT NOT NULL COMMENT '业务单ID（订单ID/充值ID）',
    biz_order_no VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
    biz_amount DECIMAL(10, 2) NOT NULL COMMENT '业务金额（订单金额/充值金额）',
    execution_status VARCHAR(32) NOT NULL COMMENT '执行状态：SUCCESS-成功, FAILED-失败, SKIPPED-跳过',
    reward_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '奖励金额（赠送金额/优惠金额）',
    reward_result_id VARCHAR(256) DEFAULT NULL COMMENT '奖励结果ID（券ID列表/账本流水ID，多个用逗号分隔）',
    failure_reason TEXT DEFAULT NULL COMMENT '失败原因',
    executed_at DATETIME DEFAULT NULL COMMENT '执行时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key),
    INDEX idx_tenant_campaign (tenant_id, campaign_id),
    INDEX idx_tenant_user (tenant_id, user_id),
    INDEX idx_biz_order (biz_order_id),
    INDEX idx_execution_status (execution_status),
    INDEX idx_executed_at (executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动执行日志表（幂等控制）';
