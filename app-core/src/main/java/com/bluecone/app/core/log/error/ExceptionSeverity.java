package com.bluecone.app.core.log.error;

/**
 * 异常严重级别，便于下游筛选与聚合。
 */
public enum ExceptionSeverity {
    INFO,
    WARN,
    ERROR,
    CRITICAL
}
