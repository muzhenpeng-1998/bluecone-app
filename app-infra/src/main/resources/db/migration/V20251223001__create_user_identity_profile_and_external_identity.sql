-- 用户核心表：身份表 + 画像表 + 外部身份绑定表
-- 创建时间：2025-12-23
-- 说明：支持多租户用户体系，外部身份绑定表用于微信 openId 等兜底唯一标识

-- ============================================================
-- 1. 用户身份表 bc_user_identity
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_user_identity (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID（内部主键，自增）',
    
    -- 唯一标识（可为空，用于跨租户识别）
    union_id            VARCHAR(128)    DEFAULT NULL COMMENT '微信 UnionId（可为空，用于跨租户识别同一用户）',
    
    -- 手机号（可为空，允许未授权手机号的用户）
    phone               VARCHAR(32)     DEFAULT NULL COMMENT '手机号（可为空）',
    country_code        VARCHAR(8)      DEFAULT '+86' COMMENT '国家区号（默认 +86）',
    
    -- 邮箱（可为空）
    email               VARCHAR(128)    DEFAULT NULL COMMENT '邮箱（可为空）',
    
    -- 注册渠道
    register_channel    VARCHAR(64)     NOT NULL COMMENT '注册渠道：WECHAT_MINI、WECHAT_H5、ADMIN 等',
    
    -- 用户状态
    status              INT             NOT NULL DEFAULT 1 COMMENT '用户状态：1=正常、0=禁用、-1=删除',
    
    -- 首次注册租户ID（用于追踪用户来源）
    first_tenant_id     BIGINT          DEFAULT NULL COMMENT '首次注册租户ID（用户首次注册时的租户）',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_union_id (union_id) COMMENT 'UnionId 唯一约束（允许 NULL）',
    UNIQUE KEY uk_phone (phone, country_code) COMMENT '手机号唯一约束（允许 NULL）',
    KEY idx_first_tenant (first_tenant_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户身份表（平台级）';

-- ============================================================
-- 2. 用户画像表 bc_user_profile
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_user_profile (
    user_id             BIGINT          NOT NULL COMMENT '用户ID（关联 bc_user_identity.id）',
    
    -- 基本信息
    nickname            VARCHAR(128)    DEFAULT NULL COMMENT '昵称',
    avatar_url          VARCHAR(512)    DEFAULT NULL COMMENT '头像 URL',
    gender              INT             DEFAULT 0 COMMENT '性别：0=未知、1=男、2=女',
    birthday            DATE            DEFAULT NULL COMMENT '生日',
    
    -- 地理信息
    city                VARCHAR(64)     DEFAULT NULL COMMENT '城市',
    province            VARCHAR(64)     DEFAULT NULL COMMENT '省份',
    country             VARCHAR(64)     DEFAULT NULL COMMENT '国家',
    language            VARCHAR(32)     DEFAULT 'zh_CN' COMMENT '语言',
    
    -- 标签（JSON 格式）
    tags_json           TEXT            DEFAULT NULL COMMENT '用户标签（JSON 数组，如 ["VIP", "高消费"]）',
    
    -- 最后登录时间
    last_login_at       DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (user_id),
    KEY idx_last_login (last_login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像表（平台级）';

-- ============================================================
-- 3. 用户外部身份绑定表 bc_user_external_identity
-- ============================================================
-- 说明：用于绑定微信 openId、支付宝 userId 等外部身份标识
--       当 unionId 为空时，使用 (provider, app_id, open_id) 作为兜底唯一标识
CREATE TABLE IF NOT EXISTS bc_user_external_identity (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '绑定ID（内部主键，自增）',
    
    -- 外部身份提供方
    provider            VARCHAR(32)     NOT NULL COMMENT '身份提供方：WECHAT_MINI、WECHAT_H5、ALIPAY 等',
    
    -- 外部应用标识
    app_id              VARCHAR(64)     NOT NULL COMMENT '外部应用ID（微信小程序 appId、支付宝 appId 等）',
    
    -- 外部用户标识
    open_id             VARCHAR(128)    NOT NULL COMMENT '外部用户ID（微信 openId、支付宝 userId 等）',
    
    -- UnionId（可为空，用于关联）
    union_id            VARCHAR(128)    DEFAULT NULL COMMENT '外部 UnionId（可为空，微信 unionId、支付宝 userId 等）',
    
    -- 关联的平台用户ID
    user_id             BIGINT          NOT NULL COMMENT '平台用户ID（关联 bc_user_identity.id）',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_app_open (provider, app_id, open_id) COMMENT '外部身份唯一约束（provider + app_id + open_id）',
    KEY idx_user_id (user_id) COMMENT '用户ID索引',
    KEY idx_union_id (union_id) COMMENT 'UnionId 索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户外部身份绑定表';

-- ============================================================
-- 4. 初始化说明
-- ============================================================
-- 说明：
-- 1. bc_user_identity：平台用户身份表，union_id 和 phone 均允许 NULL
--    - union_id 用于跨租户识别同一用户（微信 unionId）
--    - phone 用于手机号登录（用户可能不授权手机号）
--    - 当 union_id 和 phone 均为空时，使用 bc_user_external_identity 表作为兜底唯一标识
-- 2. bc_user_profile：用户画像表，user_id 为主键，与 bc_user_identity 一对一关系
-- 3. bc_user_external_identity：外部身份绑定表，用于绑定微信 openId、支付宝 userId 等
--    - 唯一约束：(provider, app_id, open_id) 确保同一外部身份只能绑定一个平台用户
--    - 用于解决"unionId 为空 + 用户不授权手机号"的识别问题
-- 4. 用户识别优先级：
--    - 优先：union_id（跨租户识别）
--    - 次选：phone + country_code（手机号登录）
--    - 兜底：bc_user_external_identity (provider, app_id, open_id)

