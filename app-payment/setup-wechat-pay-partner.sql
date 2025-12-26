-- =====================================================
-- 微信支付 V3 服务商模式配置脚本
-- 用于设置支付渠道配置和子商户信息
-- =====================================================

-- 1. 插入支付渠道配置（示例）
-- 注意：需要根据实际情况修改 tenant_id、store_id、sub_mch_id 等字段

INSERT INTO bc_payment_channel_config (
    tenant_id,
    store_id,
    channel_type,
    enabled,
    notify_url,
    wechat_secrets,
    created_at,
    updated_at
) VALUES (
    1,  -- 租户 ID（需要修改）
    1,  -- 门店 ID（需要修改）
    'WECHAT_JSAPI',  -- 渠道类型
    1,  -- 启用状态（1=启用，0=禁用）
    'https://your-domain.com/open-api/wechat/pay/notify',  -- 回调地址（可选，为空则用 defaultNotifyUrl）
    JSON_OBJECT(
        'subMchId', '1234567890',  -- 子商户号（必填，需要修改）
        'channelMode', 'SERVICE_PROVIDER'  -- 渠道模式（服务商）
    ),
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    enabled = 1,
    notify_url = 'https://your-domain.com/open-api/wechat/pay/notify',
    wechat_secrets = JSON_OBJECT(
        'subMchId', '1234567890',
        'channelMode', 'SERVICE_PROVIDER'
    ),
    updated_at = NOW();

-- 2. 查询支付渠道配置
SELECT 
    id,
    tenant_id,
    store_id,
    channel_type,
    enabled,
    notify_url,
    wechat_secrets,
    created_at,
    updated_at
FROM bc_payment_channel_config
WHERE channel_type = 'WECHAT_JSAPI'
ORDER BY tenant_id, store_id;

-- 3. 查询租户已授权小程序（确保 sub_appid 可获取）
SELECT 
    id,
    tenant_id,
    component_app_id,
    authorizer_app_id,
    authorization_status,
    authorized_at,
    created_at
FROM bc_wechat_authorized_app
WHERE authorization_status = 'AUTHORIZED'
ORDER BY tenant_id, authorized_at DESC;

-- 4. 检查配置完整性
-- 此查询会列出所有 WECHAT_JSAPI 渠道配置，并关联对应的授权小程序
SELECT 
    pcc.id AS channel_config_id,
    pcc.tenant_id,
    pcc.store_id,
    pcc.channel_type,
    pcc.enabled,
    JSON_EXTRACT(pcc.wechat_secrets, '$.subMchId') AS sub_mch_id,
    JSON_EXTRACT(pcc.wechat_secrets, '$.channelMode') AS channel_mode,
    pcc.notify_url,
    waa.authorizer_app_id AS sub_app_id,
    waa.authorization_status,
    CASE 
        WHEN pcc.enabled = 0 THEN '❌ 渠道未启用'
        WHEN JSON_EXTRACT(pcc.wechat_secrets, '$.subMchId') IS NULL THEN '❌ 子商户号未配置'
        WHEN waa.authorizer_app_id IS NULL THEN '❌ 租户未授权小程序'
        WHEN waa.authorization_status != 'AUTHORIZED' THEN '❌ 小程序授权已取消'
        ELSE '✅ 配置完整'
    END AS status_check
FROM bc_payment_channel_config pcc
LEFT JOIN bc_wechat_authorized_app waa 
    ON pcc.tenant_id = waa.tenant_id 
    AND waa.authorization_status = 'AUTHORIZED'
WHERE pcc.channel_type = 'WECHAT_JSAPI'
ORDER BY pcc.tenant_id, pcc.store_id;

-- 5. 批量插入多个租户的配置（示例）
-- 注意：需要根据实际情况修改租户和子商户信息

-- INSERT INTO bc_payment_channel_config (tenant_id, store_id, channel_type, enabled, notify_url, wechat_secrets, created_at, updated_at)
-- VALUES
--     (1, 1, 'WECHAT_JSAPI', 1, NULL, JSON_OBJECT('subMchId', '1234567890', 'channelMode', 'SERVICE_PROVIDER'), NOW(), NOW()),
--     (2, 2, 'WECHAT_JSAPI', 1, NULL, JSON_OBJECT('subMchId', '0987654321', 'channelMode', 'SERVICE_PROVIDER'), NOW(), NOW()),
--     (3, 3, 'WECHAT_JSAPI', 1, NULL, JSON_OBJECT('subMchId', '1122334455', 'channelMode', 'SERVICE_PROVIDER'), NOW(), NOW())
-- ON DUPLICATE KEY UPDATE
--     enabled = VALUES(enabled),
--     wechat_secrets = VALUES(wechat_secrets),
--     updated_at = NOW();

-- 6. 更新子商户号（如果已有配置）
-- UPDATE bc_payment_channel_config
-- SET wechat_secrets = JSON_SET(
--         wechat_secrets,
--         '$.subMchId', '1234567890',  -- 新的子商户号
--         '$.channelMode', 'SERVICE_PROVIDER'
--     ),
--     updated_at = NOW()
-- WHERE tenant_id = 1
--   AND store_id = 1
--   AND channel_type = 'WECHAT_JSAPI';

-- 7. 启用/禁用支付渠道
-- UPDATE bc_payment_channel_config
-- SET enabled = 1,  -- 1=启用，0=禁用
--     updated_at = NOW()
-- WHERE tenant_id = 1
--   AND store_id = 1
--   AND channel_type = 'WECHAT_JSAPI';

-- 8. 查看支付订单和回调日志（用于排查问题）
-- SELECT 
--     po.id,
--     po.tenant_id,
--     po.store_id,
--     po.out_trade_no,
--     po.transaction_id,
--     po.status,
--     po.channel,
--     po.method,
--     po.payable_amount,
--     po.paid_at,
--     po.created_at
-- FROM bc_payment_order po
-- WHERE po.channel = 'WECHAT'
--   AND po.method = 'WECHAT_JSAPI'
-- ORDER BY po.created_at DESC
-- LIMIT 20;

-- =====================================================
-- 常见问题排查
-- =====================================================

-- Q1: 下单时提示"子商户号配置缺失"
-- A1: 检查 bc_payment_channel_config.wechat_secrets 中是否有 subMchId 字段
-- SELECT 
--     tenant_id,
--     store_id,
--     JSON_EXTRACT(wechat_secrets, '$.subMchId') AS sub_mch_id
-- FROM bc_payment_channel_config
-- WHERE channel_type = 'WECHAT_JSAPI';

-- Q2: 下单时提示"该租户未授权小程序"
-- A2: 检查 bc_wechat_authorized_app 中是否有 AUTHORIZED 状态的记录
-- SELECT 
--     tenant_id,
--     authorizer_app_id,
--     authorization_status,
--     authorized_at
-- FROM bc_wechat_authorized_app
-- WHERE tenant_id = 1;  -- 替换为实际租户 ID

-- Q3: 回调时提示"支付单不存在"
-- A3: 检查 out_trade_no 是否正确，是否能解析出 paymentId
-- SELECT 
--     id,
--     out_trade_no,
--     status
-- FROM bc_payment_order
-- WHERE out_trade_no = 'PAY_123456';  -- 替换为实际 out_trade_no

-- Q4: 回调时提示"金额不一致"
-- A4: 检查回调金额（分）与订单金额是否匹配
-- SELECT 
--     id,
--     out_trade_no,
--     payable_amount,
--     payable_amount * 100 AS amount_in_fen
-- FROM bc_payment_order
-- WHERE out_trade_no = 'PAY_123456';  -- 替换为实际 out_trade_no

