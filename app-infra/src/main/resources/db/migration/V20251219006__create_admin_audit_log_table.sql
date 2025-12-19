-- 商户后台审计日志表
-- 创建时间：2025-12-19
-- 说明：记录所有后台管理操作的审计日志，包含操作前后的数据快照，用于合规审计和问题追溯

-- ============================================================
-- 后台审计日志表 bc_admin_audit_log
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_admin_audit_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '日志ID（自增主键）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID（租户隔离）',
    
    -- 操作人信息
    operator_id         BIGINT          NOT NULL COMMENT '操作人ID（用户ID）',
    operator_name       VARCHAR(128)    DEFAULT NULL COMMENT '操作人姓名（冗余，便于查询）',
    operator_ip         VARCHAR(64)     DEFAULT NULL COMMENT '操作人IP地址',
    
    -- 操作信息
    action              VARCHAR(64)     NOT NULL COMMENT '操作类型（如：CREATE、UPDATE、DELETE、ONLINE、OFFLINE、GRANT）',
    resource_type       VARCHAR(64)     NOT NULL COMMENT '资源类型（如：STORE、PRODUCT、ORDER、COUPON_TEMPLATE、WALLET）',
    resource_id         BIGINT          DEFAULT NULL COMMENT '资源ID（如：门店ID、商品ID、订单ID等）',
    resource_name       VARCHAR(255)    DEFAULT NULL COMMENT '资源名称（冗余，便于查询）',
    
    -- 操作详情
    operation_desc      VARCHAR(512)    DEFAULT NULL COMMENT '操作描述（如：修改商品价格、上线优惠券模板）',
    request_uri         VARCHAR(255)    DEFAULT NULL COMMENT '请求URI',
    request_method      VARCHAR(16)     DEFAULT NULL COMMENT '请求方法（GET、POST、PUT、DELETE）',
    
    -- 数据快照（JSON格式）
    data_before         TEXT            DEFAULT NULL COMMENT '操作前数据快照（JSON格式，仅UPDATE/DELETE操作有值）',
    data_after          TEXT            DEFAULT NULL COMMENT '操作后数据快照（JSON格式，CREATE/UPDATE操作有值）',
    change_summary      TEXT            DEFAULT NULL COMMENT '变更摘要（JSON格式，记录具体变更的字段和值）',
    
    -- 操作结果
    status              VARCHAR(32)     NOT NULL DEFAULT 'SUCCESS' COMMENT '操作状态：SUCCESS、FAILED',
    error_message       VARCHAR(512)    DEFAULT NULL COMMENT '错误信息（操作失败时记录）',
    
    -- 请求上下文
    trace_id            VARCHAR(64)     DEFAULT NULL COMMENT '链路追踪ID',
    user_agent          VARCHAR(512)    DEFAULT NULL COMMENT '用户代理（浏览器信息）',
    
    -- 扩展字段
    ext_json            TEXT            DEFAULT NULL COMMENT '扩展信息JSON',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    
    PRIMARY KEY (id),
    KEY idx_tenant_operator (tenant_id, operator_id, created_at DESC),
    KEY idx_tenant_resource (tenant_id, resource_type, resource_id, created_at DESC),
    KEY idx_tenant_action (tenant_id, action, created_at DESC),
    KEY idx_created_at (created_at),
    KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台审计日志表';

-- ============================================================
-- 索引说明
-- ============================================================
-- idx_tenant_operator: 按操作人查询审计日志（支持查看某个管理员的所有操作）
-- idx_tenant_resource: 按资源查询审计日志（支持查看某个资源的所有变更历史）
-- idx_tenant_action: 按操作类型查询审计日志（支持查看某类操作的所有记录）
-- idx_created_at: 按时间查询审计日志（支持时间范围查询）
-- idx_trace_id: 按链路追踪ID查询（支持关联请求链路）
