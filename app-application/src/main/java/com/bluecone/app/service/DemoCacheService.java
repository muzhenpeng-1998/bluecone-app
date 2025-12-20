package com.bluecone.app.service;

import com.bluecone.app.infra.cache.annotation.CacheEvict;
import com.bluecone.app.infra.cache.annotation.Cached;
import com.bluecone.app.infra.cache.core.CacheKey;
import com.bluecone.app.infra.cache.profile.CacheProfileName;
import com.bluecone.app.core.tenant.TenantContext;
import org.springframework.stereotype.Service;

/**
 * Demo service for cache testing.
 */
@Service
public class DemoCacheService {

    @Cached(profile = CacheProfileName.ORDER_DETAIL)
    public DemoValue load(Long id) {
        String tenantId = TenantContext.getTenantId();
        return new DemoValue(id, tenantId, "loaded-" + System.currentTimeMillis());
    }

    @CacheEvict(profile = CacheProfileName.ORDER_DETAIL)
    public DemoValue update(Long id, String payload) {
        String tenantId = TenantContext.getTenantId();
        return new DemoValue(id, tenantId, payload != null ? payload : "updated-" + System.currentTimeMillis());
    }

    public static class DemoValue {
        private final Long id;
        private final String tenantId;
        private final String payload;

        public DemoValue(Long id, String tenantId, String payload) {
            this.id = id;
            this.tenantId = tenantId;
            this.payload = payload;
        }

        public Long getId() {
            return id;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getPayload() {
            return payload;
        }
    }
}

