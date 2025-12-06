-- 支付模块核心表

CREATE TABLE IF NOT EXISTS bc_payment_order (
    id                 BIGINT        NOT NULL COMMENT '支付单ID，主键',
    tenant_id          BIGINT        NOT NULL COMMENT '租户ID',
    store_id           BIGINT        NOT NULL COMMENT '门店ID',
    business_order_id  BIGINT        NOT NULL COMMENT '业务订单ID（关联订单模块的订单ID）',
    user_id            BIGINT        NOT NULL COMMENT '平台用户ID（bc_user_identity 等）',
    pay_channel        VARCHAR(32)   NOT NULL COMMENT '支付渠道：WECHAT_JSAPI, WECHAT_NATIVE, ALIPAY_WAP, CASH 等',
    pay_scene          VARCHAR(32)   NOT NULL COMMENT '支付场景：MINI_DINE_IN, MINI_TAKEAWAY, POS 等',
    currency           VARCHAR(8)    NOT NULL DEFAULT 'CNY' COMMENT '币种',
    total_amount       DECIMAL(18,2) NOT NULL COMMENT '订单总金额（原价总额）',
    pay_amount         DECIMAL(18,2) NOT NULL COMMENT '实际支付金额（优惠后）',
    discount_amount    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '优惠金额（券/满减/会员价等，总和）',
    status             VARCHAR(32)   NOT NULL COMMENT '支付状态：INIT, PENDING, SUCCESS, FAILED, CLOSED, REFUND_PENDING, REFUNDED 等',
    client_ip          VARCHAR(64)   DEFAULT NULL COMMENT '客户端IP（可脱敏存储）',
    user_agent         VARCHAR(255)  DEFAULT NULL COMMENT 'User-Agent 截断信息',
    expire_time        DATETIME      DEFAULT NULL COMMENT '支付单过期时间（超时未支付自动关闭）',
    third_app_id       VARCHAR(64)   DEFAULT NULL COMMENT '渠道侧AppId（如微信小程序AppId）',
    third_mch_id       VARCHAR(64)   DEFAULT NULL COMMENT '渠道侧商户号',
    third_sub_mch_id   VARCHAR(64)   DEFAULT NULL COMMENT '渠道侧子商户号（服务商模式）',
    third_trade_no     VARCHAR(64)   DEFAULT NULL COMMENT '渠道侧交易号（如微信transaction_id）',
    notify_success     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已成功通知下游业务：0=否，1=是',
    version            BIGINT        NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by         BIGINT        DEFAULT NULL COMMENT '创建人',
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    updated_by         BIGINT        DEFAULT NULL COMMENT '最后更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_business_channel (tenant_id, business_order_id, pay_channel),
    KEY idx_tenant_status_created (tenant_id, status, created_at),
    KEY idx_third_trade_no (third_trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付单';

CREATE TABLE IF NOT EXISTS bc_payment_channel_config (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
    store_id        BIGINT       NOT NULL COMMENT '门店ID',
    channel_type    VARCHAR(32)  NOT NULL COMMENT '渠道类型：WECHAT, ALIPAY 等',
    channel_mode    VARCHAR(32)  NOT NULL COMMENT '接入模式：SERVICE_PROVIDER（服务商）、DIRECT（普通商户）',
    app_id          VARCHAR(64)  DEFAULT NULL COMMENT '应用AppId',
    mch_id          VARCHAR(64)  DEFAULT NULL COMMENT '商户号',
    sub_mch_id      VARCHAR(64)  DEFAULT NULL COMMENT '子商户号（服务商模式用）',
    encrypt_payload TEXT         NOT NULL COMMENT '加密后的配置JSON（apiV3Key、证书序列号等敏感信息）',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    remark          VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_store_channel (tenant_id, store_id, channel_type),
    KEY idx_tenant_channel_status (tenant_id, channel_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付渠道配置';

CREATE TABLE IF NOT EXISTS bc_payment_refund (
    id               BIGINT        NOT NULL COMMENT '退款单ID，主键',
    tenant_id        BIGINT        NOT NULL COMMENT '租户ID',
    payment_order_id BIGINT        NOT NULL COMMENT '关联的支付单ID',
    refund_no        VARCHAR(64)   NOT NULL COMMENT '平台退款单号（对接第三方refund_no）',
    third_refund_no  VARCHAR(64)   DEFAULT NULL COMMENT '渠道退款单号',
    refund_amount    DECIMAL(18,2) NOT NULL COMMENT '本次退款金额',
    status           VARCHAR(32)   NOT NULL COMMENT '退款状态：REFUND_PENDING, REFUNDED, REFUND_FAILED 等',
    reason           VARCHAR(255)  DEFAULT NULL COMMENT '退款原因（用户申请/商户取消/系统自动等）',
    notify_success   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已通知下游业务',
    version          BIGINT        NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by       BIGINT        DEFAULT NULL COMMENT '创建人',
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    updated_by       BIGINT        DEFAULT NULL COMMENT '最后更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_refund_no (tenant_id, refund_no),
    KEY idx_tenant_payment (tenant_id, payment_order_id),
    KEY idx_third_refund_no (third_refund_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付退款单';

CREATE TABLE IF NOT EXISTS bc_payment_notify_log (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id         BIGINT       DEFAULT NULL COMMENT '租户ID（如果能从回调中解析到则填充，否则为空）',
    channel_type      VARCHAR(32)  NOT NULL COMMENT '渠道类型：WECHAT, ALIPAY 等',
    notify_type       VARCHAR(32)  NOT NULL COMMENT '通知类型：PAY, REFUND 等',
    business_order_id BIGINT       DEFAULT NULL COMMENT '关联业务订单ID',
    payment_order_id  BIGINT       DEFAULT NULL COMMENT '关联支付单ID',
    raw_body          TEXT         NOT NULL COMMENT '原始回调报文（必要时可做部分脱敏）',
    handle_status     VARCHAR(32)  NOT NULL DEFAULT 'RECEIVED' COMMENT '处理状态：RECEIVED, SUCCESS, FAILED',
    handle_error      VARCHAR(512) DEFAULT NULL COMMENT '处理失败原因（如有）',
    remote_ip         VARCHAR(64)  DEFAULT NULL COMMENT '回调来源IP',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',
    PRIMARY KEY (id),
    KEY idx_channel_type_time (channel_type, created_at),
    KEY idx_payment_order (payment_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付回调日志';

CREATE TABLE IF NOT EXISTS bc_payment_reconcile_record (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id      BIGINT       NOT NULL COMMENT '租户ID',
    channel_type   VARCHAR(32)  NOT NULL COMMENT '渠道类型：WECHAT, ALIPAY 等',
    bill_date      DATE         NOT NULL COMMENT '账单日期（渠道账单对应的日期）',
    file_name      VARCHAR(255) DEFAULT NULL COMMENT '账单文件名称或下载标识',
    status         VARCHAR(32)  NOT NULL COMMENT '对账状态：INIT, DOWNLOADED, RECONCILED, FAILED',
    mismatch_count INT          NOT NULL DEFAULT 0 COMMENT '对账差异条目数量',
    remark         VARCHAR(255) DEFAULT NULL COMMENT '备注/失败原因摘要',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_channel_billdate (tenant_id, channel_type, bill_date),
    KEY idx_tenant_status (tenant_id, status, bill_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付对账记录';
