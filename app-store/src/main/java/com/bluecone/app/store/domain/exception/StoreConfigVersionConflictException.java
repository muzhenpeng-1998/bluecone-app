package com.bluecone.app.store.domain.exception;

/**
 * 配置版本号乐观锁冲突异常。
 * <p>高并发写入时，若 config_version 不匹配，则抛出该异常通知调用方重试或刷新配置。</p>
 */
public class StoreConfigVersionConflictException extends RuntimeException {

    public StoreConfigVersionConflictException(String message) {
        super(message);
    }

    public StoreConfigVersionConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
