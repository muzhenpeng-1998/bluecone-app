-- 充值单表：添加渠道交易号唯一索引
-- 创建时间：2025-12-19
-- 说明：支持基于渠道交易号的幂等性检查，防止同一笔支付回调重复入账

-- 添加渠道交易号唯一索引（用于支付回调幂等）
ALTER TABLE bc_wallet_recharge_order 
ADD UNIQUE KEY uk_tenant_pay_no (tenant_id, pay_no);

-- 索引说明：
-- uk_tenant_pay_no: 保证同一租户下，同一渠道交易号只能关联一个充值单
--                   用于支付回调幂等性检查
