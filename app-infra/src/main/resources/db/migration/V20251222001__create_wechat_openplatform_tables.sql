-- =====================================================
-- 微信开放平台相关表
-- 用于存储第三方平台凭证与授权小程序信息
-- =====================================================

-- 第三方平台（Component）凭证表
-- 存储 component_verify_ticket 与 component_access_token
CREATE TABLE IF NOT EXISTS bc_wechat_component_credential (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    component_app_id VARCHAR(64) NOT NULL COMMENT '第三方平台 AppID',
    component_app_secret VARCHAR(128) NOT NULL COMMENT '第三方平台 AppSecret',
    component_verify_ticket VARCHAR(512) DEFAULT NULL COMMENT '微信推送的 verify_ticket（每 10 分钟推送一次）',
    component_access_token VARCHAR(512) DEFAULT NULL COMMENT '第三方平台 access_token',
    component_access_token_expire_at DATETIME DEFAULT NULL COMMENT 'component_access_token 过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_component_app_id (component_app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信第三方平台凭证表';

-- 已授权小程序表
-- 存储租户授权给第三方平台的小程序信息
CREATE TABLE IF NOT EXISTS bc_wechat_authorized_app (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '租户 ID',
    component_app_id VARCHAR(64) NOT NULL COMMENT '第三方平台 AppID',
    authorizer_app_id VARCHAR(64) NOT NULL COMMENT '授权方（小程序）AppID',
    authorizer_refresh_token VARCHAR(512) DEFAULT NULL COMMENT '授权方刷新令牌（用于刷新 authorizer_access_token）',
    authorizer_access_token VARCHAR(512) DEFAULT NULL COMMENT '授权方接口调用令牌',
    authorizer_access_token_expire_at DATETIME DEFAULT NULL COMMENT 'authorizer_access_token 过期时间',
    
    -- 小程序基本信息（从 getAuthorizerInfo 获取）
    nick_name VARCHAR(128) DEFAULT NULL COMMENT '小程序昵称',
    head_img VARCHAR(512) DEFAULT NULL COMMENT '小程序头像',
    service_type_info INT DEFAULT NULL COMMENT '小程序类型（0=订阅号，1=由历史老账号升级后的订阅号，2=服务号）',
    verify_type_info INT DEFAULT NULL COMMENT '认证类型（-1=未认证，0=微信认证，1=新浪微博认证，2=腾讯微博认证，3=已资质认证通过但还未通过名称认证，4=已资质认证通过、还未通过名称认证，但通过了新浪微博认证，5=已资质认证通过、还未通过名称认证，但通过了腾讯微博认证）',
    user_name VARCHAR(128) DEFAULT NULL COMMENT '原始 ID',
    principal_name VARCHAR(256) DEFAULT NULL COMMENT '主体名称',
    alias VARCHAR(128) DEFAULT NULL COMMENT '小程序别名',
    qrcode_url VARCHAR(512) DEFAULT NULL COMMENT '二维码图片 URL',
    business_info TEXT DEFAULT NULL COMMENT '功能的开通状况（JSON 格式）',
    mini_program_info TEXT DEFAULT NULL COMMENT '小程序配置信息（JSON 格式）',
    
    -- 授权状态
    authorization_status VARCHAR(32) NOT NULL DEFAULT 'AUTHORIZED' COMMENT '授权状态：AUTHORIZED=已授权，UNAUTHORIZED=已取消授权',
    authorized_at DATETIME DEFAULT NULL COMMENT '授权时间',
    unauthorized_at DATETIME DEFAULT NULL COMMENT '取消授权时间',
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_authorizer_app (tenant_id, authorizer_app_id),
    KEY idx_component_app_id (component_app_id),
    KEY idx_authorizer_app_id (authorizer_app_id),
    KEY idx_authorization_status (authorization_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信已授权小程序表';

