package com.bluecone.app.infra.redis.core;

/**
 * Redis 业务命名空间，保持 key 逻辑分组一致。
 */
public enum RedisKeyNamespace {

    SESSION("session"),
    ORDER("order"),
    USER("user"),
    TENANT("tenant"),
    CACHE("cache"),
    IDEMPOTENT("idempotent"),
    LOCK("lock"),
    RATE_LIMIT("rate_limit");

    private final String code;

    RedisKeyNamespace(String code) {
        this.code = code;
    }

    /**
     * 返回拼 key 使用的小写命名空间编码。
     *
     * @return 命名空间编码
     */
    public String code() {
        return code;
    }
}
