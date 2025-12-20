-- =====================================================
-- 统一消息中心初始化脚本
-- 插入系统级通知模板
-- =====================================================

-- =====================================================
-- 1. 账单支付成功通知（站内信）
-- =====================================================
INSERT INTO bc_notify_template (
    tenant_id,
    template_code,
    template_name,
    biz_type,
    channel,
    title_template,
    content_template,
    template_variables,
    status,
    priority,
    rate_limit_config,
    created_by
) VALUES (
    NULL,
    'INVOICE_PAID_REMINDER',
    '账单支付成功提醒（站内信）',
    'INVOICE_PAID',
    'IN_APP',
    '账单支付成功',
    '您的账单 {{invoiceNo}} 已支付成功！\n\n订阅计划：{{planName}}\n支付金额：{{amount}} 元\n生效期限：{{effectiveStartAt}} 至 {{effectiveEndAt}}\n\n感谢您的支持！',
    '[
        {"name": "invoiceNo", "type": "string", "description": "账单号"},
        {"name": "planName", "type": "string", "description": "订阅计划名称"},
        {"name": "amount", "type": "string", "description": "支付金额（元）"},
        {"name": "effectiveStartAt", "type": "string", "description": "生效开始时间"},
        {"name": "effectiveEndAt", "type": "string", "description": "生效结束时间"}
    ]',
    'ENABLED',
    60,
    '{"dailyLimit": 5, "quietHoursStart": "22:00", "quietHoursEnd": "08:00"}',
    'system'
);

-- =====================================================
-- 2. 账单支付成功通知（邮件）
-- =====================================================
INSERT INTO bc_notify_template (
    tenant_id,
    template_code,
    template_name,
    biz_type,
    channel,
    title_template,
    content_template,
    template_variables,
    status,
    priority,
    rate_limit_config,
    created_by
) VALUES (
    NULL,
    'INVOICE_PAID_REMINDER',
    '账单支付成功提醒（邮件）',
    'INVOICE_PAID',
    'EMAIL',
    '账单支付成功 - BlueCone',
    '尊敬的用户：

您的账单 {{invoiceNo}} 已支付成功！

订阅计划：{{planName}}
支付金额：{{amount}} 元
生效期限：{{effectiveStartAt}} 至 {{effectiveEndAt}}

您现在可以享受完整的服务功能。如有任何问题，请联系客服。

感谢您的支持！

BlueCone 团队',
    '[
        {"name": "invoiceNo", "type": "string", "description": "账单号"},
        {"name": "planName", "type": "string", "description": "订阅计划名称"},
        {"name": "amount", "type": "string", "description": "支付金额（元）"},
        {"name": "effectiveStartAt", "type": "string", "description": "生效开始时间"},
        {"name": "effectiveEndAt", "type": "string", "description": "生效结束时间"}
    ]',
    'ENABLED',
    60,
    '{"dailyLimit": 5, "quietHoursStart": "22:00", "quietHoursEnd": "08:00"}',
    'system'
);

-- =====================================================
-- 3. 续费成功通知（站内信）
-- =====================================================
INSERT INTO bc_notify_template (
    tenant_id,
    template_code,
    template_name,
    biz_type,
    channel,
    title_template,
    content_template,
    template_variables,
    status,
    priority,
    rate_limit_config,
    created_by
) VALUES (
    NULL,
    'RENEWAL_SUCCESS',
    '续费成功通知（站内信）',
    'RENEWAL_SUCCESS',
    'IN_APP',
    '订阅续费成功',
    '您的订阅计划 {{planName}} 已成功续费！\n\n新的到期时间：{{newEndDate}}\n\n感谢您继续使用我们的服务！',
    '[
        {"name": "planName", "type": "string", "description": "订阅计划名称"},
        {"name": "newEndDate", "type": "string", "description": "新的到期时间"}
    ]',
    'ENABLED',
    50,
    '{"dailyLimit": 3, "quietHoursStart": "22:00", "quietHoursEnd": "08:00"}',
    'system'
);

-- =====================================================
-- 4. 订单待取货通知（站内信）
-- =====================================================
INSERT INTO bc_notify_template (
    tenant_id,
    template_code,
    template_name,
    biz_type,
    channel,
    title_template,
    content_template,
    template_variables,
    status,
    priority,
    rate_limit_config,
    created_by
) VALUES (
    NULL,
    'ORDER_READY',
    '订单待取货通知（站内信）',
    'ORDER_READY',
    'IN_APP',
    '您的订单已备货完成',
    '您的订单 {{orderNo}} 已备货完成！\n\n商品名称：{{productName}}\n门店名称：{{storeName}}\n\n请尽快到店取货，感谢您的光临！',
    '[
        {"name": "orderNo", "type": "string", "description": "订单号"},
        {"name": "productName", "type": "string", "description": "商品名称"},
        {"name": "storeName", "type": "string", "description": "门店名称"}
    ]',
    'ENABLED',
    70,
    '{"dailyLimit": 20}',
    'system'
);

-- =====================================================
-- 5. 退款成功通知（站内信）
-- =====================================================
INSERT INTO bc_notify_template (
    tenant_id,
    template_code,
    template_name,
    biz_type,
    channel,
    title_template,
    content_template,
    template_variables,
    status,
    priority,
    rate_limit_config,
    created_by
) VALUES (
    NULL,
    'REFUND_SUCCESS',
    '退款成功通知（站内信）',
    'REFUND_SUCCESS',
    'IN_APP',
    '退款已成功',
    '您的订单 {{orderNo}} 退款已成功！\n\n退款金额：{{refundAmount}} 元\n\n退款将在 1-3 个工作日内到账，请注意查收。',
    '[
        {"name": "orderNo", "type": "string", "description": "订单号"},
        {"name": "refundAmount", "type": "string", "description": "退款金额（元）"}
    ]',
    'ENABLED',
    50,
    '{"dailyLimit": 10, "quietHoursStart": "22:00", "quietHoursEnd": "08:00"}',
    'system'
);

-- =====================================================
-- 使用说明：
-- 
-- 1. 在应用启动后运行此脚本，初始化系统级通知模板
-- 2. template_code + channel + tenant_id 必须唯一
-- 3. tenant_id = NULL 表示系统级模板，所有租户共享
-- 4. 租户可创建自己的模板覆盖系统模板（tenant_id != NULL）
-- 5. template_variables 为 JSON 数组，定义模板变量元数据
-- 6. rate_limit_config 为频控配置，可选
-- 
-- 查询已创建的模板：
-- SELECT template_code, template_name, channel, status 
-- FROM bc_notify_template 
-- WHERE tenant_id IS NULL 
-- ORDER BY biz_type, channel;
-- =====================================================
