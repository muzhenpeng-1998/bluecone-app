-- 调度中心表结构
CREATE TABLE IF NOT EXISTS bc_scheduler_job
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(128) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    cron_expr       VARCHAR(255) NOT NULL,
    enabled         TINYINT      NOT NULL DEFAULT 1,
    timeout_seconds INT          NOT NULL DEFAULT 60,
    tenant_id       VARCHAR(64)  NOT NULL DEFAULT 'default',
    last_run_at     TIMESTAMP NULL,
    next_run_at     TIMESTAMP NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_job_code_tenant (code, tenant_id),
    INDEX idx_job_next_run (next_run_at)
);

CREATE TABLE IF NOT EXISTS bc_scheduler_job_execution_log
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_code      VARCHAR(128) NOT NULL,
    trace_id      VARCHAR(64),
    tenant_id     VARCHAR(64)  NOT NULL DEFAULT 'default',
    status        VARCHAR(16)  NOT NULL,
    started_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at   TIMESTAMP    NULL DEFAULT NULL,
    duration_ms   BIGINT,
    error_message TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_job_code_created (job_code, created_at)
);
