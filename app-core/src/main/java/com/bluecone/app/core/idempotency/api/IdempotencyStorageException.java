package com.bluecone.app.core.idempotency.api;

/**
 * 幂等存储层异常，用于包装底层 DB/Redis 出错情况。
 */
public class IdempotencyStorageException extends RuntimeException {

    public IdempotencyStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

