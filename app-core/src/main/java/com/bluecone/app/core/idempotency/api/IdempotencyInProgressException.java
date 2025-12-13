package com.bluecone.app.core.idempotency.api;

/**
 * 幂等请求仍在处理中，且当前策略选择抛异常而非返回 inProgress 标记。
 */
public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException(String message) {
        super(message);
    }
}

