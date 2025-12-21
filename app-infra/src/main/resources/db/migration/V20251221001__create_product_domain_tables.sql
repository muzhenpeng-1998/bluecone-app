-- ============================================================
-- Flyway Migration: Product Domain Tables
-- Version: V20251221001
-- Description: Create/Align all product domain tables with global standards
-- Author: System
-- Date: 2025-12-21
-- ============================================================

-- ============================================================
-- 1. bc_product_category (商品分类)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    public_id VARCHAR(32) NULL COMMENT '对外公开ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID,0表示顶级',
    name VARCHAR(64) NOT NULL COMMENT '分类名称',
    type INT NOT NULL DEFAULT 1 COMMENT '分类类型:1商品菜单,2服务菜单,3场馆',
    level INT NOT NULL DEFAULT 1 COMMENT '层级:1一级,2二级',
    icon VARCHAR(512) NULL COMMENT '分类图标URL',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值,越大越靠前',
    remark VARCHAR(256) NULL COMMENT '备注',
    display_start_at DATETIME NULL COMMENT '展示开始时间',
    display_end_at DATETIME NULL COMMENT '展示结束时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_tenant_status_sort (tenant_id, status, sort_order),
    INDEX idx_tenant_parent (tenant_id, parent_id),
    INDEX idx_deleted (deleted),
    INDEX idx_display_time (tenant_id, display_start_at, display_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- ============================================================
-- 2. bc_product_category_rel (商品分类关联)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_category_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1有效,0无效',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_tenant_category_product (tenant_id, category_id, product_id),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_tenant_category (tenant_id, category_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类关联表';

-- ============================================================
-- 3. bc_product (商品SPU)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    public_id VARCHAR(32) NULL COMMENT '对外公开ID',
    product_code VARCHAR(64) NULL COMMENT '商品编码',
    name VARCHAR(128) NOT NULL COMMENT '商品名称',
    subtitle VARCHAR(256) NULL COMMENT '副标题',
    product_type INT NOT NULL DEFAULT 1 COMMENT '商品类型:1餐饮,2服务,3场馆,4储值,5会员,6券包',
    description TEXT NULL COMMENT '商品描述',
    main_image VARCHAR(512) NULL COMMENT '主图URL',
    media_gallery JSON NULL COMMENT '媒体资源JSON',
    unit VARCHAR(16) NULL COMMENT '销售单位',
    status INT NOT NULL DEFAULT 0 COMMENT '状态:0草稿,1启用,-1禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    product_meta JSON NULL COMMENT '扩展字段JSON',
    display_start_at DATETIME NULL COMMENT '展示开始时间',
    display_end_at DATETIME NULL COMMENT '展示结束时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    UNIQUE KEY uk_tenant_product_code (tenant_id, product_code),
    INDEX idx_tenant_status_sort (tenant_id, status, sort_order),
    INDEX idx_deleted (deleted),
    INDEX idx_display_time (tenant_id, display_start_at, display_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SPU表';

-- ============================================================
-- 4. bc_product_sku (商品SKU)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'SKU ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    public_id VARCHAR(32) NULL COMMENT '对外公开ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(64) NULL COMMENT 'SKU编码',
    name VARCHAR(128) NOT NULL COMMENT 'SKU名称',
    base_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '基础售价',
    market_price DECIMAL(10,2) NULL COMMENT '划线价',
    cost_price DECIMAL(10,2) NULL COMMENT '成本价',
    barcode VARCHAR(64) NULL COMMENT '条形码',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认SKU:1是,0否',
    spec_combination JSON NULL COMMENT '规格组合JSON',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用,-1删除',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    sku_meta JSON NULL COMMENT '扩展字段JSON',
    display_start_at DATETIME NULL COMMENT '展示开始时间',
    display_end_at DATETIME NULL COMMENT '展示结束时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    UNIQUE KEY uk_tenant_sku_code (tenant_id, sku_code),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_tenant_status_sort (tenant_id, status, sort_order),
    INDEX idx_deleted (deleted),
    INDEX idx_display_time (tenant_id, display_start_at, display_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU表';

-- ============================================================
-- 5. bc_product_spec_group (规格组)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_spec_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规格组ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    public_id VARCHAR(32) NULL COMMENT '对外公开ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    name VARCHAR(64) NOT NULL COMMENT '规格组名称',
    select_type INT NOT NULL DEFAULT 1 COMMENT '选择类型:1单选,2多选',
    required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必选:1是,0否',
    max_select INT NULL COMMENT '最大选择数量',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规格组表';

-- ============================================================
-- 6. bc_product_spec_option (规格选项)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_spec_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规格项ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    public_id VARCHAR(32) NULL COMMENT '对外公开ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    spec_group_id BIGINT NOT NULL COMMENT '规格组ID',
    name VARCHAR(64) NOT NULL COMMENT '规格项名称',
    price_delta DECIMAL(10,2) NULL DEFAULT 0.00 COMMENT '价格增减',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认:1是,0否',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_tenant_group (tenant_id, spec_group_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规格选项表';

-- ============================================================
-- 7. bc_product_attr_group (属性组素材库)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_attr_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '属性组ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    public_id VARCHAR(32) NULL COMMENT '对外公开ID',
    name VARCHAR(64) NOT NULL COMMENT '属性组名称',
    scope INT NOT NULL DEFAULT 1 COMMENT '作用范围:1口味,2制作,3展示标签',
    select_type INT NOT NULL DEFAULT 1 COMMENT '选择类型:1单选,2多选',
    required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必选:1是,0否',
    max_select INT NULL COMMENT '最大可选数量',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    remark VARCHAR(256) NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='属性组素材库';

-- ============================================================
-- 8. bc_product_attr_option (属性选项素材库)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_attr_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '属性项ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    public_id VARCHAR(32) NULL COMMENT '对外公开ID',
    attr_group_id BIGINT NOT NULL COMMENT '属性组ID',
    name VARCHAR(64) NOT NULL COMMENT '属性项名称',
    value_code VARCHAR(32) NULL COMMENT '值编码',
    price_delta DECIMAL(10,2) NULL DEFAULT 0.00 COMMENT '价格增减',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    INDEX idx_tenant_group (tenant_id, attr_group_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='属性选项素材库';

-- ============================================================
-- 9. bc_product_attr_group_rel (商品绑定属性组 - 新增)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_attr_group_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    attr_group_id BIGINT NOT NULL COMMENT '属性组ID',
    required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必选:1是,0否',
    min_select INT NULL DEFAULT 0 COMMENT '最小选择数量',
    max_select INT NULL COMMENT '最大选择数量',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    display_start_at DATETIME NULL COMMENT '展示开始时间',
    display_end_at DATETIME NULL COMMENT '展示结束时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_tenant_product_group (tenant_id, product_id, attr_group_id),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_tenant_group (tenant_id, attr_group_id),
    INDEX idx_deleted (deleted),
    INDEX idx_display_time (tenant_id, display_start_at, display_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品绑定属性组表';

-- ============================================================
-- 10. bc_product_attr_rel (商品绑定属性选项 - 覆盖规则)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_attr_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    attr_group_id BIGINT NOT NULL COMMENT '属性组ID',
    attr_option_id BIGINT NOT NULL COMMENT '属性选项ID',
    price_delta_override DECIMAL(10,2) NULL COMMENT '价格增减覆盖',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_tenant_product_option (tenant_id, product_id, attr_option_id),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_tenant_group (tenant_id, attr_group_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品绑定属性选项表';

-- ============================================================
-- 11. bc_addon_group (小料组素材库)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_addon_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '小料组ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    public_id VARCHAR(32) NULL COMMENT '对外公开ID',
    name VARCHAR(64) NOT NULL COMMENT '小料组名称',
    type INT NOT NULL DEFAULT 1 COMMENT '类型:1计价小料,2不计价小料',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    remark VARCHAR(256) NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小料组素材库';

-- ============================================================
-- 12. bc_addon_item (小料项素材库)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_addon_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '小料项ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    public_id VARCHAR(32) NULL COMMENT '对外公开ID',
    group_id BIGINT NOT NULL COMMENT '小料组ID',
    name VARCHAR(64) NOT NULL COMMENT '小料名称',
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单份价格',
    cost_price DECIMAL(10,2) NULL COMMENT '成本价',
    max_quantity DECIMAL(10,2) NULL COMMENT '最多可选数量',
    free_limit DECIMAL(10,2) NULL DEFAULT 0.00 COMMENT '前N份免费',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    INDEX idx_tenant_group (tenant_id, group_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小料项素材库';

-- ============================================================
-- 13. bc_product_addon_group_rel (商品绑定小料组 - 新增)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_addon_group_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    addon_group_id BIGINT NOT NULL COMMENT '小料组ID',
    required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必选:1是,0否',
    min_select INT NULL DEFAULT 0 COMMENT '最小选择数量',
    max_select INT NULL COMMENT '最大选择数量',
    max_total_quantity DECIMAL(10,2) NULL COMMENT '总可选上限',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    display_start_at DATETIME NULL COMMENT '展示开始时间',
    display_end_at DATETIME NULL COMMENT '展示结束时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_tenant_product_group (tenant_id, product_id, addon_group_id),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_tenant_group (tenant_id, addon_group_id),
    INDEX idx_deleted (deleted),
    INDEX idx_display_time (tenant_id, display_start_at, display_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品绑定小料组表';

-- ============================================================
-- 14. bc_product_addon_rel (商品绑定小料项 - 覆盖规则)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_addon_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    addon_group_id BIGINT NOT NULL COMMENT '小料组ID',
    addon_item_id BIGINT NOT NULL COMMENT '小料项ID',
    price_override DECIMAL(10,2) NULL COMMENT '价格覆盖',
    max_quantity_override DECIMAL(10,2) NULL COMMENT '最大数量覆盖',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_tenant_product_item (tenant_id, product_id, addon_item_id),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_tenant_group (tenant_id, addon_group_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品绑定小料项表';

-- ============================================================
-- 15. bc_product_store_config (商品门店可售配置)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_product_store_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NULL COMMENT 'SKU ID',
    channel VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '售卖渠道',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见:1是,0否',
    override_price DECIMAL(10,2) NULL COMMENT '门店价覆盖',
    available_order_types JSON NULL COMMENT '可用订单类型JSON',
    available_time_ranges JSON NULL COMMENT '可售时间段JSON',
    daily_sold_out_limit INT NULL COMMENT '每日销量上限',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    display_start_at DATETIME NULL COMMENT '展示开始时间',
    display_end_at DATETIME NULL COMMENT '展示结束时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    sku_id_normalized BIGINT AS (COALESCE(sku_id, 0)) STORED COMMENT 'SKU ID归一化值(用于唯一索引)',
    
    UNIQUE KEY uk_tenant_store_product_channel (tenant_id, store_id, product_id, channel, sku_id_normalized),
    INDEX idx_tenant_store (tenant_id, store_id),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_deleted (deleted),
    INDEX idx_display_time (tenant_id, display_start_at, display_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品门店可售配置表';

-- ============================================================
-- 16. bc_store_menu_snapshot (门店菜单快照)
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_store_menu_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    channel VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '渠道',
    order_scene VARCHAR(32) NOT NULL DEFAULT 'DEFAULT' COMMENT '场景',
    version BIGINT NOT NULL DEFAULT 1 COMMENT '版本号',
    menu_json MEDIUMTEXT NOT NULL COMMENT '菜单快照JSON',
    generated_at DATETIME NOT NULL COMMENT '生成时间',
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1有效,0无效',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_tenant_store_channel_scene (tenant_id, store_id, channel, order_scene),
    INDEX idx_tenant_store (tenant_id, store_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店菜单快照表';

-- ============================================================
-- End of Migration
-- ============================================================

