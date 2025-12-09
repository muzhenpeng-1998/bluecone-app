-- 资源中心模块表结构（供 app-resource 模块使用）
-- 包含资源物理对象表与资源绑定表

CREATE TABLE IF NOT EXISTS bc_res_object (
    id               BIGINT        NOT NULL COMMENT '主键ID，使用雪花ID',
    tenant_id        BIGINT        NOT NULL COMMENT '归属租户ID',
    profile_code     VARCHAR(64)   NOT NULL COMMENT '资源配置档案编码，如 STORE_LOGO、PRODUCT_IMAGE',
    storage_provider VARCHAR(32)   NOT NULL COMMENT '存储提供商：ALIYUN_OSS、TENCENT_COS、MINIO、LOCAL 等',
    storage_key      VARCHAR(512)  NOT NULL COMMENT '底层对象Key，如 prod/tenant-1/product/image/2025/12/xxx.webp',
    size_bytes       BIGINT        NOT NULL COMMENT '文件大小（字节）',
    content_type     VARCHAR(128)           NULL COMMENT 'MIME类型',
    file_ext         VARCHAR(32)            NULL COMMENT '文件扩展名（小写，如 jpg, png, webp）',
    hash_sha256      CHAR(64)               NULL COMMENT '文件内容哈希（SHA-256），用于秒传/去重',
    access_level     TINYINT       NOT NULL COMMENT '访问级别：1-PRIVATE，2-PUBLIC_READ，3-INTERNAL',
    status           TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1-有效，0-失效',
    ext_json         JSON                   NULL COMMENT '扩展字段（缩略图信息、处理流水线等）',
    created_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_hash (hash_sha256),
    KEY idx_profile (tenant_id, profile_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源物理对象表';

CREATE TABLE IF NOT EXISTS bc_res_binding (
    id                 BIGINT       NOT NULL COMMENT '主键ID，使用雪花ID',
    owner_type         VARCHAR(32)  NOT NULL COMMENT '业务归属类型：STORE / PRODUCT / ORDER / USER / TENANT / SYSTEM 等',
    owner_id           BIGINT       NOT NULL COMMENT '业务对象ID，如 store_id、product_id',
    purpose            VARCHAR(64)  NOT NULL COMMENT '用途：MAIN_LOGO, GALLERY, BANNER, AVATAR, DETAIL_IMAGE 等',
    resource_object_id BIGINT       NOT NULL COMMENT '关联的资源物理对象ID，对应 bc_res_object.id',
    sort_order         INT          NOT NULL DEFAULT 0 COMMENT '排序号（同一 owner+purpose 内）',
    is_main            TINYINT      NOT NULL DEFAULT 0 COMMENT '是否主图/主资源标记：1-是，0-否',
    created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    created_by         BIGINT                NULL COMMENT '操作人（可选）',
    PRIMARY KEY (id),
    KEY idx_owner (owner_type, owner_id, purpose),
    KEY idx_resource (resource_object_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源绑定表（业务对象与资源关联）';

