package com.bluecone.app.ops.api.dto.cacheinval;

import java.util.List;

public record CacheInvalSummary(
        long totalEvents,
        List<ScopeStat> eventsByScope,
        List<NamespaceStat> eventsByNamespace,
        List<TenantStat> topTenants,
        List<StormItem> storms,
        List<DecisionStat> eventsByDecision
) {
    public record ScopeStat(String scope, long count) {
    }

    public record NamespaceStat(String namespace, long count) {
    }

    public record TenantStat(long tenantId, long count) {
    }

    public record StormItem(long tenantId,
                            String scope,
                            String namespace,
                            long countPerMinute,
                            long thresholdPerMinute) {
    }

    public record DecisionStat(String decision, long count) {
    }
}
