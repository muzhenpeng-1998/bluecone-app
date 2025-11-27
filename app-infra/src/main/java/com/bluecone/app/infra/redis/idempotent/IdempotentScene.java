package com.bluecone.app.infra.redis.idempotent;

/**
 * 幂等场景标识，用于日志区分及 key 维度扩展。
 */
public enum IdempotentScene {
    API,
    MQ,
    SCHEDULED
}
