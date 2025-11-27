package com.bluecone.app.infra.redis.ratelimit;

/**
 * 限流触发后的处理策略。
 */
public enum RateLimitStrategy {
    /**
     * 直接拒绝并抛业务异常。
     */
    REJECT,
    /**
     * 静默丢弃请求，返回 null，谨慎使用。
     */
    SILENT_DROP,
    /**
     * 预留 fallback 扩展。
     */
    FALLBACK
}
