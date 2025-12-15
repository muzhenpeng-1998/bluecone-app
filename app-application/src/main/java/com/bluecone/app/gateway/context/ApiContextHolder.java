package com.bluecone.app.gateway.context;

import com.bluecone.app.gateway.ApiContext;

/**
 * ThreadLocal holder for ApiContext within gateway lifecycle.
 * 
 * <p>This delegates to the core version and also updates the core context holder
 * for use by modules that depend only on app-core.</p>
 */
public final class ApiContextHolder {

    private static final ThreadLocal<ApiContext> CTX = new ThreadLocal<>();

    private ApiContextHolder() {
    }

    public static void set(ApiContext context) {
        CTX.set(context);
        // Also set the core context for modules that only depend on app-core
        if (context != null) {
            com.bluecone.app.core.gateway.ApiContextHolder.set(context.toCoreContext());
        } else {
            com.bluecone.app.core.gateway.ApiContextHolder.clear();
        }
    }

    public static ApiContext get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
        com.bluecone.app.core.gateway.ApiContextHolder.clear();
    }
}

