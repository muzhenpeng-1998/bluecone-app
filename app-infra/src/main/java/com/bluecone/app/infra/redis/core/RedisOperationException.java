package com.bluecone.app.infra.redis.core;

/**
 * Redis 操作异常的运行时包装，携带操作上下文。
 */
public class RedisOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String operation;
    private final String key;

    public RedisOperationException(String operation, String key, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.key = key;
    }

    public RedisOperationException(String operation, String key, String message) {
        super(message);
        this.operation = operation;
        this.key = key;
    }

    /**
     * Redis 命令名称（SET/GET/HSET...）。
     */
    public String getOperation() {
        return operation;
    }

    /**
     * 失败操作关联的 Redis key，可为空。
     */
    public String getKey() {
        return key;
    }
}
