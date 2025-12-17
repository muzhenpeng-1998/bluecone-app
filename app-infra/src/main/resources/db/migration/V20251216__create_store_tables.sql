-- Flyway 迁移脚本：创建门店（Store）相关表
-- 说明：此脚本创建门店主表及其配置子表，包含完整的中文字段注释和索引
-- 版本：V20251216
-- 数据库：MySQL 8.0+

-- ============================================
-- 1. 门店主表（bc_store）
-- ============================================
CREATE TABLE IF NOT EXISTS bc_store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '门店主键 ID（自增）',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID，用于多租户隔离',
    
    -- ID 相关字段
    internal_id BINARY(16) NULL COMMENT '内部主键 ULID128（BINARY(16)），用于跨系统唯一标识',
    public_id VARCHAR(40) NULL COMMENT '对外门店 ID（PublicId 格式：sto_{ulid}），供外部系统调用',
    store_no BIGINT UNSIGNED NULL COMMENT '门店数字编号（Snowflake long），用于业务展示和排序',
    store_code VARCHAR(64) NULL COMMENT '门店编码（业务自定义编码，租户内唯一）',
    
    -- 基础信息
    name VARCHAR(255) NOT NULL COMMENT '门店名称',
    short_name VARCHAR(128) NULL COMMENT '门店简称',
    industry_type VARCHAR(32) NULL COMMENT '行业类型（COFFEE/FOOD/BAKERY/OTHER）',
    brand_id BIGINT NULL COMMENT '品牌 ID（关联品牌表，可选）',
    
    -- 地址信息
    province_code VARCHAR(32) NULL COMMENT '省份编码',
    city_code VARCHAR(32) NULL COMMENT '城市编码',
    district_code VARCHAR(32) NULL COMMENT '区县编码',
    address VARCHAR(512) NULL COMMENT '详细地址',
    longitude DECIMAL(10, 7) NULL COMMENT '经度（用于地图定位）',
    latitude DECIMAL(10, 7) NULL COMMENT '纬度（用于地图定位）',
    
    -- 联系方式
    contact_phone VARCHAR(32) NULL COMMENT '联系电话',
    
    -- 媒体资源
    logo_url VARCHAR(512) NULL COMMENT '门店 Logo URL',
    cover_url VARCHAR(512) NULL COMMENT '门店封面图 URL',
    
    -- 状态与配置
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT '门店状态（OPEN/PAUSED/CLOSED）',
    onboard_status TINYINT NULL COMMENT '入驻状态：0-草稿，1-可营业（READY），2-关闭/停业（CLOSED）',
    miniapp_appid VARCHAR(64) NULL COMMENT '门店级绑定的小程序 appid（当门店与租户默认小程序不一致时可单独覆盖）',
    open_for_orders TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可接单（true=可接单，false=暂停接单）',
    config_version BIGINT NOT NULL DEFAULT 1 COMMENT '配置版本号（乐观锁字段，每次配置更新时递增）',
    
    -- 扩展字段
    ext_json TEXT NULL COMMENT '扩展配置 JSON（用于存储业务自定义字段）',
    
    -- 审计字段
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT NULL COMMENT '创建人 ID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by BIGINT NULL COMMENT '更新人 ID',
    
    -- 软删除
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除（0=未删除，1=已删除）',
    
    -- 索引
    UNIQUE KEY uk_bc_store_internal_id (internal_id) COMMENT '内部 ID 唯一索引',
    UNIQUE KEY uk_bc_store_tenant_public_id (tenant_id, public_id) COMMENT '租户+对外 ID 唯一索引（支持按租户约束对外 ID 唯一性）',
    UNIQUE KEY uk_bc_store_tenant_store_code (tenant_id, store_code) COMMENT '租户+门店编码唯一索引（支持租户内门店编码唯一）',
    KEY idx_bc_store_tenant_id (tenant_id) COMMENT '租户 ID 索引（用于多租户查询）',
    KEY idx_bc_store_status (status) COMMENT '门店状态索引（用于状态筛选）',
    KEY idx_bc_store_city_code (city_code) COMMENT '城市编码索引（用于按城市查询）',
    KEY idx_bc_store_industry_type (industry_type) COMMENT '行业类型索引（用于按行业筛选）',
    KEY idx_bc_store_created_at (created_at) COMMENT '创建时间索引（用于时间排序）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店主表';

-- ============================================
-- 2. 门店能力配置表（bc_store_capability）
-- ============================================
CREATE TABLE IF NOT EXISTS bc_store_capability (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '能力配置主键 ID（自增）',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    store_id BIGINT NOT NULL COMMENT '门店 ID（关联 bc_store.id）',
    
    capability VARCHAR(64) NOT NULL COMMENT '能力标识（如 DINE_IN-堂食、TAKE_OUT-外卖、PICKUP-自取、RESERVATION-预约等）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用该能力（true=启用，false=禁用）',
    config_json TEXT NULL COMMENT '能力扩展配置 JSON（如外卖配送范围、预约规则等）',
    
    -- 审计字段
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT NULL COMMENT '创建人 ID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by BIGINT NULL COMMENT '更新人 ID',
    
    -- 软删除
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除（0=未删除，1=已删除）',
    
    -- 索引
    UNIQUE KEY uk_bc_store_capability_store_capability (tenant_id, store_id, capability, is_deleted) COMMENT '租户+门店+能力唯一索引（同一门店同一能力只能有一条有效记录）',
    KEY idx_bc_store_capability_store_id (tenant_id, store_id) COMMENT '租户+门店索引（用于查询门店所有能力）',
    KEY idx_bc_store_capability_enabled (enabled) COMMENT '启用状态索引（用于筛选启用的能力）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店能力配置表（支持门店启用/禁用特定业务能力）';

-- ============================================
-- 3. 门店常规营业时间表（bc_store_opening_hours）
-- ============================================
CREATE TABLE IF NOT EXISTS bc_store_opening_hours (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '营业时间主键 ID（自增）',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    store_id BIGINT NOT NULL COMMENT '门店 ID（关联 bc_store.id）',
    
    weekday TINYINT NOT NULL COMMENT '星期几（1=周一，2=周二，...，7=周日）',
    start_time TIME NOT NULL COMMENT '当日开始营业时间（格式：HH:mm:ss）',
    end_time TIME NOT NULL COMMENT '当日结束营业时间（格式：HH:mm:ss）',
    period_type VARCHAR(32) NULL COMMENT '时间段类型（如 FULL_DAY-全天、MULTI_PERIOD-多时段等）',
    
    -- 审计字段
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT NULL COMMENT '创建人 ID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by BIGINT NULL COMMENT '更新人 ID',
    
    -- 软删除
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除（0=未删除，1=已删除）',
    
    -- 索引
    KEY idx_bc_store_opening_hours_store_id (tenant_id, store_id) COMMENT '租户+门店索引（用于查询门店所有营业时间）',
    KEY idx_bc_store_opening_hours_weekday (weekday) COMMENT '星期几索引（用于按星期查询）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店常规营业时间表（按星期几配置常规营业时段）';

-- ============================================
-- 4. 门店特殊日配置表（bc_store_special_day）
-- ============================================
CREATE TABLE IF NOT EXISTS bc_store_special_day (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '特殊日配置主键 ID（自增）',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    store_id BIGINT NOT NULL COMMENT '门店 ID（关联 bc_store.id）',
    
    date DATE NOT NULL COMMENT '特殊日期（格式：YYYY-MM-DD）',
    special_type VARCHAR(32) NOT NULL COMMENT '特殊类型（如 HOLIDAY-节假日延长营业、CLOSED-临时停业、ADJUSTED-临时调整等）',
    start_time TIME NULL COMMENT '特殊日开始时间（格式：HH:mm:ss，NULL 表示全天）',
    end_time TIME NULL COMMENT '特殊日结束时间（格式：HH:mm:ss，NULL 表示全天）',
    remark VARCHAR(512) NULL COMMENT '备注信息（方便运营同学了解配置原因）',
    
    -- 审计字段
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT NULL COMMENT '创建人 ID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by BIGINT NULL COMMENT '更新人 ID',
    
    -- 软删除
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除（0=未删除，1=已删除）',
    
    -- 索引
    UNIQUE KEY uk_bc_store_special_day_store_date (tenant_id, store_id, date, is_deleted) COMMENT '租户+门店+日期唯一索引（同一门店同一日期只能有一条有效记录）',
    KEY idx_bc_store_special_day_store_id (tenant_id, store_id) COMMENT '租户+门店索引（用于查询门店所有特殊日）',
    KEY idx_bc_store_special_day_date (date) COMMENT '日期索引（用于按日期查询）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店特殊日配置表（用于配置节假日、临时停业等特殊营业安排）';

-- ============================================
-- 5. 门店渠道绑定表（bc_store_channel）
-- ============================================
CREATE TABLE IF NOT EXISTS bc_store_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '渠道绑定主键 ID（自增）',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    store_id BIGINT NOT NULL COMMENT '门店 ID（关联 bc_store.id）',
    
    channel_type VARCHAR(64) NOT NULL COMMENT '渠道类型（如 WECHAT_MINI-微信小程序、ALIPAY_MINI-支付宝小程序、APP-APP 等）',
    external_store_id VARCHAR(128) NULL COMMENT '外部平台门店 ID（在第三方平台的店铺标识）',
    app_id VARCHAR(64) NULL COMMENT '小程序 appid（当渠道为小程序时使用）',
    config_json TEXT NULL COMMENT '渠道扩展配置 JSON（如渠道特定参数、授权信息等）',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '渠道状态（ACTIVE-启用、INACTIVE-禁用、SUSPENDED-暂停）',
    
    -- 审计字段
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT NULL COMMENT '创建人 ID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by BIGINT NULL COMMENT '更新人 ID',
    
    -- 软删除
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除（0=未删除，1=已删除）',
    
    -- 索引
    UNIQUE KEY uk_bc_store_channel_store_channel (tenant_id, store_id, channel_type, is_deleted) COMMENT '租户+门店+渠道类型唯一索引（同一门店同一渠道只能有一条有效记录）',
    KEY idx_bc_store_channel_store_id (tenant_id, store_id) COMMENT '租户+门店索引（用于查询门店所有渠道）',
    KEY idx_bc_store_channel_status (status) COMMENT '渠道状态索引（用于筛选启用的渠道）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店渠道绑定表（用于管理门店在不同渠道的绑定关系）';

-- ============================================
-- 6. 门店只读快照表（bc_store_read_model）
-- ============================================
-- 说明：此表用于事件驱动架构中的读模型，支持消费端幂等
CREATE TABLE IF NOT EXISTS bc_store_read_model (
    store_internal_id BINARY(16) NOT NULL COMMENT '门店内部主键（ULID128/BINARY(16)），作为主键',
    public_id VARCHAR(40) NOT NULL COMMENT '对外门店 ID（sto_{ulid}）',
    store_no BIGINT UNSIGNED NULL COMMENT '门店数字编号（Snowflake long）',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    store_name VARCHAR(255) NOT NULL COMMENT '门店名称',
    updated_at DATETIME NOT NULL COMMENT '最近一次快照更新时间',
    
    PRIMARY KEY (store_internal_id),
    KEY idx_bc_store_read_model_tenant_id (tenant_id) COMMENT '租户 ID 索引',
    KEY idx_bc_store_read_model_public_id (public_id) COMMENT '对外 ID 索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店只读快照表（消费端幂等演示，用于事件驱动架构）';

