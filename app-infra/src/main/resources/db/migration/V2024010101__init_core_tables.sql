CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32),
    created_at DATETIME NULL,
    updated_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_auth_session (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    client_type VARCHAR(32),
    device_id VARCHAR(64),
    refresh_token_hash VARCHAR(256),
    refresh_token_expire_at DATETIME NULL,
    status VARCHAR(32),
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    last_active_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_test (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64),
    biz_key VARCHAR(128),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bc_config_property (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,
    value_type VARCHAR(32),
    scope VARCHAR(32),
    env VARCHAR(32),
    tenant_id BIGINT,
    enabled TINYINT(1) DEFAULT 1,
    version INT DEFAULT 0,
    remark VARCHAR(255),
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    KEY idx_config_key_scope (config_key, scope, tenant_id, env)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
