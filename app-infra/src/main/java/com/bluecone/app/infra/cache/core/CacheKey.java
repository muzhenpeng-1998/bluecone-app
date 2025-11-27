package com.bluecone.app.infra.cache.core;

import java.util.Objects;
import java.util.Optional;

/**
 * CacheKey 聚合租户、领域、业务主键，保证所有缓存键可预测且可审计。
 */
public final class CacheKey {

    private static final String PREFIX = "bluecone";

    private final String tenantId;
    private final String domain;
    private final String bizId;
    private final String version;

    private CacheKey(String tenantId, String domain, String bizId, String version) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.domain = Objects.requireNonNull(domain, "domain");
        this.bizId = Objects.requireNonNull(bizId, "bizId");
        this.version = version;
    }

    public static CacheKey forUserProfile(String tenantId, String userId) {
        return new CacheKey(tenantId, "user", userId, null);
    }

    public static CacheKey forOrderDetail(String tenantId, Long orderId) {
        return new CacheKey(tenantId, "order", String.valueOf(orderId), null);
    }

    public static CacheKey generic(String tenantId, String domain, Object bizId) {
        return new CacheKey(tenantId, domain, String.valueOf(bizId), null);
    }

    public CacheKey withVersion(String version) {
        return new CacheKey(tenantId, domain, bizId, version);
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDomain() {
        return domain;
    }

    public String getBizId() {
        return bizId;
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * 输出统一的 Redis key 规范，便于跨服务排查。
     */
    public String toRedisKey() {
        String base = String.join(":", PREFIX, tenantId, domain, bizId);
        return version == null ? base : base + ":v" + version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheKey cacheKey)) return false;
        return tenantId.equals(cacheKey.tenantId)
                && domain.equals(cacheKey.domain)
                && bizId.equals(cacheKey.bizId)
                && Objects.equals(version, cacheKey.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, domain, bizId, version);
    }

    @Override
    public String toString() {
        return toRedisKey();
    }
}
