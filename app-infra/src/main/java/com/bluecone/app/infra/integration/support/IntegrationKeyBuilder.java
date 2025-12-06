package com.bluecone.app.infra.integration.support;

/**
 * 统一构建 Integration Hub 相关的 key/id。
 */
public final class IntegrationKeyBuilder {

    private static final String SUBSCRIPTION_CACHE_PREFIX = "integration:sub:";
    private static final String DELIVERY_KEY_PREFIX = "integration:delivery:";
    private static final String RATE_LIMIT_PREFIX = "integration:ratelimit:";

    public String subscriptionCacheKey(final Long tenantId, final String eventType) {
        return SUBSCRIPTION_CACHE_PREFIX + (tenantId == null ? "none" : tenantId) + ":" + eventType;
    }

    public String deliveryIdempotentKey(final String eventId, final Long subscriptionId) {
        return DELIVERY_KEY_PREFIX + eventId + ":" + subscriptionId;
    }

    public String rateLimitKey(final Long subscriptionId) {
        return RATE_LIMIT_PREFIX + subscriptionId;
    }
}
