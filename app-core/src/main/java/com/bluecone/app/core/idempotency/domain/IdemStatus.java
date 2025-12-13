package com.bluecone.app.core.idempotency.domain;

/**
 * 幂等记录状态。
 */
public enum IdemStatus {

    /**
     * 正在处理。
     */
    PROCESSING,

    /**
     * 已成功完成。
     */
    SUCCEEDED,

    /**
     * 处理失败。
     */
    FAILED
}

