package com.bluecone.app.gateway.context;

import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;

/**
 * Convenience accessors for current request context.
 */
public final class RuntimeContextUtil {

    private RuntimeContextUtil() {
    }

    public static Long currentTenantId() {
        ApiContext ctx = ApiContextHolder.get();
        return ctx != null ? parseLong(ctx.getTenantId()) : null;
    }

    public static Long currentUserId() {
        ApiContext ctx = ApiContextHolder.get();
        return ctx != null ? ctx.getUserId() : null;
    }

    public static Long currentStoreId() {
        ApiContext ctx = ApiContextHolder.get();
        return ctx != null ? ctx.getStoreId() : null;
    }

    public static StoreOrderSnapshot currentStoreOrderSnapshot() {
        ApiContext ctx = ApiContextHolder.get();
        return ctx != null ? ctx.getStoreOrderSnapshot() : null;
    }

    private static Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

