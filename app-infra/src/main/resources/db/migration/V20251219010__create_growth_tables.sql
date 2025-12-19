-- ===================================================================
-- Migration: V20251219010__create_growth_tables.sql
-- Description: 增长引擎：邀新归因、奖励发放、活动管理
-- Author: System
-- Date: 2025-12-19
-- ===================================================================

-- 增长活动表
CREATE TABLE IF NOT EXISTS bc_growth_campaign (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '活动ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    campaign_code VARCHAR(64) NOT NULL COMMENT '活动编码（唯一）',
    campaign_name VARCHAR(128) NOT NULL COMMENT '活动名称',
    campaign_type VARCHAR(32) NOT NULL DEFAULT 'INVITE' COMMENT '活动类型：INVITE-邀新',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态：DRAFT-草稿, ACTIVE-进行中, PAUSED-已暂停, ENDED-已结束',
    rules_json TEXT NOT NULL COMMENT '奖励规则（JSON格式：{inviterRewards:[{type,value}], inviteeRewards:[{type,value}]}）',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '活动结束时间（NULL表示长期有效）',
    description TEXT DEFAULT NULL COMMENT '活动描述',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_tenant_code (tenant_id, campaign_code),
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_time_range (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='增长活动表';

-- 邀请码表
CREATE TABLE IF NOT EXISTS bc_growth_invite_code (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '邀请码记录ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    campaign_code VARCHAR(64) NOT NULL COMMENT '活动编码',
    invite_code VARCHAR(32) NOT NULL COMMENT '邀请码（唯一）',
    inviter_user_id BIGINT NOT NULL COMMENT '邀请人用户ID',
    invites_count INT NOT NULL DEFAULT 0 COMMENT '邀请人数',
    successful_invites_count INT NOT NULL DEFAULT 0 COMMENT '成功邀请人数（完成首单）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_invite_code (invite_code),
    UNIQUE KEY uk_tenant_campaign_inviter (tenant_id, campaign_code, inviter_user_id),
    INDEX idx_tenant_inviter (tenant_id, inviter_user_id),
    INDEX idx_campaign (campaign_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

-- 归因关系表
CREATE TABLE IF NOT EXISTS bc_growth_attribution (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '归因ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    campaign_code VARCHAR(64) NOT NULL COMMENT '活动编码',
    invite_code VARCHAR(32) NOT NULL COMMENT '邀请码',
    inviter_user_id BIGINT NOT NULL COMMENT '邀请人用户ID',
    invitee_user_id BIGINT NOT NULL COMMENT '被邀请人用户ID',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '归因状态：PENDING-待确认, CONFIRMED-已确认（完成首单）, INVALID-已失效',
    bound_at DATETIME NOT NULL COMMENT '绑定时间',
    confirmed_at DATETIME DEFAULT NULL COMMENT '确认时间（首单完成时间）',
    first_order_id BIGINT DEFAULT NULL COMMENT '首单订单ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_tenant_campaign_invitee (tenant_id, campaign_code, invitee_user_id),
    INDEX idx_tenant_inviter (tenant_id, inviter_user_id),
    INDEX idx_invitee (invitee_user_id),
    INDEX idx_invite_code (invite_code),
    INDEX idx_status (status),
    INDEX idx_first_order (first_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='归因关系表';

-- 奖励发放日志表
CREATE TABLE IF NOT EXISTS bc_growth_reward_issue_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '发放记录ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    campaign_code VARCHAR(64) NOT NULL COMMENT '活动编码',
    idempotency_key VARCHAR(256) NOT NULL COMMENT '幂等键（全局唯一）',
    attribution_id BIGINT NOT NULL COMMENT '归因记录ID',
    user_id BIGINT NOT NULL COMMENT '奖励接收用户ID',
    user_role VARCHAR(32) NOT NULL COMMENT '用户角色：INVITER-邀请人, INVITEE-被邀请人',
    reward_type VARCHAR(32) NOT NULL COMMENT '奖励类型：COUPON-优惠券, WALLET_CREDIT-储值, POINTS-积分',
    reward_value VARCHAR(256) NOT NULL COMMENT '奖励值（JSON格式，如：{templateId:123} 或 {amount:1000} 或 {points:100}）',
    issue_status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '发放状态：PROCESSING-处理中, SUCCESS-成功, FAILED-失败',
    result_id VARCHAR(128) DEFAULT NULL COMMENT '发放结果ID（券ID/账本流水ID）',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '错误码',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    trigger_order_id BIGINT NOT NULL COMMENT '触发订单ID',
    issued_at DATETIME DEFAULT NULL COMMENT '发放成功时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key),
    INDEX idx_tenant_user (tenant_id, user_id),
    INDEX idx_campaign (campaign_code),
    INDEX idx_attribution (attribution_id),
    INDEX idx_status (issue_status),
    INDEX idx_trigger_order (trigger_order_id),
    INDEX idx_user_role (user_id, user_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖励发放日志表';
