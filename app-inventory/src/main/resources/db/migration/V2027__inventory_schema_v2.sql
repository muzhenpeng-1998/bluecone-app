SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `bc_inv_item`;
CREATE TABLE `bc_inv_item` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库存对象ID，主键',
  `tenant_id`     BIGINT       NOT NULL COMMENT '租户ID',
  `item_type`     VARCHAR(32)  NOT NULL COMMENT '库存对象类型：PRODUCT_SKU, MATERIAL, SERVICE_SLOT 等',
  `ref_id`        BIGINT       NOT NULL COMMENT '业务侧引用ID，例如商品SKU ID、物料ID等（约定为对应模块主键）',
  `external_code` VARCHAR(64)      DEFAULT NULL COMMENT '外部编码/商家编码/条码，可选',
  `name`          VARCHAR(256)     DEFAULT NULL COMMENT '展示名称快照，冗余，便于查询与报表',
  `unit`          VARCHAR(32)  NOT NULL DEFAULT 'UNIT' COMMENT '展示计量单位：PIECE, CUP, GRAM, MILLILITER, MINUTE 等',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `remark`        VARCHAR(512)     DEFAULT NULL COMMENT '备注',
  `ext`           JSON              DEFAULT NULL COMMENT '多业态扩展字段，JSON 格式',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_item_ref` (`tenant_id`, `item_type`, `ref_id`),
  KEY `idx_inv_item_tenant_type_ref` (`tenant_id`, `item_type`, `ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存对象维表：定义有库存的业务对象（租户级）';

DROP TABLE IF EXISTS `bc_inv_location`;
CREATE TABLE `bc_inv_location` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库位ID，主键',
  `tenant_id`  BIGINT       NOT NULL COMMENT '租户ID',
  `store_id`   BIGINT       NOT NULL COMMENT '门店ID',
  `code`       VARCHAR(64)  NOT NULL COMMENT '库位编码：默认DEFAULT，或FRONT/BACK/CENTRAL等',
  `name`       VARCHAR(128) NOT NULL COMMENT '库位名称：前场仓/后场仓/中央仓等',
  `type`       VARCHAR(32)  NOT NULL DEFAULT 'STORE' COMMENT '库位类型：STORE, CENTRAL, VIRTUAL 等',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `remark`     VARCHAR(512)     DEFAULT NULL COMMENT '备注',
  `ext`        JSON              DEFAULT NULL COMMENT '扩展字段',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_location` (`tenant_id`, `store_id`, `code`),
  KEY `idx_inv_location_tenant_store` (`tenant_id`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存库位表：门店内部前场/后场/中央仓等';

DROP TABLE IF EXISTS `bc_inv_stock`;
CREATE TABLE `bc_inv_stock` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT   NOT NULL COMMENT '租户ID',
  `store_id`      BIGINT   NOT NULL COMMENT '门店ID',
  `item_id`       BIGINT   NOT NULL COMMENT '库存对象ID，对应 bc_inv_item.id',
  `location_id`   BIGINT   NOT NULL DEFAULT 0 COMMENT '库位ID，对应 bc_inv_location.id，0表示默认库位',
  `total_qty`     BIGINT   NOT NULL DEFAULT 0 COMMENT '总库存数量（最小单位）',
  `locked_qty`    BIGINT   NOT NULL DEFAULT 0 COMMENT '被订单锁定的数量（最小单位）',
  `available_qty` BIGINT   NOT NULL DEFAULT 0 COMMENT '可用库存数量 = total_qty - locked_qty（最小单位）',
  `safety_stock`  BIGINT   NOT NULL DEFAULT 0 COMMENT '安全库存线（最小单位），低于此值触发预警',
  `version`       BIGINT   NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_stock_dim` (`tenant_id`, `store_id`, `item_id`, `location_id`),
  KEY `idx_inv_stock_item` (`tenant_id`, `item_id`),
  KEY `idx_inv_stock_store` (`tenant_id`, `store_id`),
  KEY `idx_inv_stock_tenant_item_store` (`tenant_id`, `item_id`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存快照表：当前总库存/锁定库存/可用库存（最小单位）';

DROP TABLE IF EXISTS `bc_inv_stock_lock`;
CREATE TABLE `bc_inv_stock_lock` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT      NOT NULL COMMENT '租户ID',
  `store_id`      BIGINT      NOT NULL COMMENT '门店ID',
  `item_id`       BIGINT      NOT NULL COMMENT '库存对象ID，对应 bc_inv_item.id',
  `location_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '库位ID，保持与 bc_inv_stock 一致',
  `order_id`      BIGINT      NOT NULL COMMENT '统一订单ID',
  `order_item_id` BIGINT           DEFAULT NULL COMMENT '订单明细ID',
  `lock_qty`      BIGINT      NOT NULL COMMENT '本次锁定数量（最小单位）',
  `status`        TINYINT     NOT NULL COMMENT '锁定状态：0=LOCKED,1=CONFIRMED,2=RELEASED,3=EXPIRED',
  `expire_at`     DATETIME         DEFAULT NULL COMMENT '锁定过期时间（未支付自动释放）',
  `request_id`    VARCHAR(64) NOT NULL COMMENT '幂等请求ID（保证同一请求不重复锁库存）',
  `remark`        VARCHAR(512)     DEFAULT NULL COMMENT '备注',
  `ext`           JSON             DEFAULT NULL COMMENT '扩展字段',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_lock_request` (`tenant_id`, `request_id`),
  KEY `idx_inv_lock_order` (`tenant_id`, `store_id`, `order_id`),
  KEY `idx_inv_lock_item_status` (`tenant_id`, `item_id`, `status`),
  KEY `idx_inv_lock_expire` (`tenant_id`, `expire_at`),
  KEY `idx_inv_lock_expire_status` (`tenant_id`, `status`, `expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存锁定记录表：订单占用库存的明细';

DROP TABLE IF EXISTS `bc_inv_txn`;
CREATE TABLE `bc_inv_txn` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT      NOT NULL COMMENT '租户ID',
  `store_id`      BIGINT      NOT NULL COMMENT '门店ID',
  `item_id`       BIGINT      NOT NULL COMMENT '库存对象ID，对应 bc_inv_item.id',
  `location_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '库位ID，对应库存维度',
  `txn_type`      VARCHAR(32) NOT NULL COMMENT '事务类型：LOCK, UNLOCK, DEDUCT, INBOUND, OUTBOUND, ADJUST 等',
  `txn_direction` VARCHAR(8)  NOT NULL COMMENT '方向：IN, OUT',
  `qty`           BIGINT      NOT NULL COMMENT '本次变动数量（最小单位，正数）',
  `before_total`  BIGINT           DEFAULT NULL COMMENT '变动前总库存（最小单位）',
  `after_total`   BIGINT           DEFAULT NULL COMMENT '变动后总库存（最小单位）',
  `before_locked` BIGINT           DEFAULT NULL COMMENT '变动前锁定库存（最小单位）',
  `after_locked`  BIGINT           DEFAULT NULL COMMENT '变动后锁定库存（最小单位）',
  `biz_ref_type`  VARCHAR(32) NOT NULL COMMENT '业务来源类型：ORDER, PURCHASE, MANUAL, INVENTORY_CHECK 等',
  `biz_ref_id`    BIGINT           DEFAULT NULL COMMENT '业务来源ID，如订单ID、采购单ID、盘点任务ID等',
  `lock_id`       BIGINT           DEFAULT NULL COMMENT '关联的库存锁定记录ID（如有）',
  `request_id`    VARCHAR(64) NOT NULL COMMENT '幂等ID（与业务请求保持一致）',
  `extra`         JSON             DEFAULT NULL COMMENT '扩展字段JSON，预留给特殊业态',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_txn_request` (`tenant_id`, `request_id`),
  KEY `idx_inv_txn_item_time` (`tenant_id`, `item_id`, `created_at`),
  KEY `idx_inv_txn_biz` (`tenant_id`, `biz_ref_type`, `biz_ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存事务流水表：所有库存变动的审计记录（建议按时间归档或分区）';

DROP TABLE IF EXISTS `bc_inv_policy`;
CREATE TABLE `bc_inv_policy` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`       BIGINT      NOT NULL COMMENT '租户ID',
  `store_id`        BIGINT      NOT NULL COMMENT '门店ID',
  `item_id`         BIGINT      NOT NULL COMMENT '库存对象ID，对应 bc_inv_item.id',
  `deduct_mode`     VARCHAR(16) NOT NULL COMMENT '扣减时机：ON_ORDER, ON_PAID, ON_CONFIRM',
  `oversell_allowed` TINYINT    NOT NULL DEFAULT 0 COMMENT '是否允许超卖：0否，1是',
  `oversell_limit`  BIGINT      NOT NULL DEFAULT 0 COMMENT '允许超卖上限（最小单位），0表示不允许超卖',
  `max_daily_sold`  BIGINT      NOT NULL DEFAULT 0 COMMENT '单日最大可售量（最小单位），0表示不限制',
  `status`          TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `remark`          VARCHAR(512)    DEFAULT NULL COMMENT '备注',
  `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_policy_item` (`tenant_id`, `store_id`, `item_id`),
  KEY `idx_inv_policy_tenant_store` (`tenant_id`, `store_id`),
  KEY `idx_inv_policy_item` (`tenant_id`, `item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存策略配置表：单品扣减/超卖/限售规则';

SET FOREIGN_KEY_CHECKS = 1;

