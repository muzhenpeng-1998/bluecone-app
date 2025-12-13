package com.bluecone.app.core.idempotency.api;

/**
 * 幂等请求冲突异常：同一幂等键对应的请求摘要不一致。
 *
 * <p>通常表示调用方误复用了 Idempotency-Key，应尽快修复调用逻辑。</p>
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }

    public IdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

