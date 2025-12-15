package com.bluecone.app.core.gateway;

/**
 * ThreadLocal holder for ApiContext within gateway lifecycle.
 */
public final class ApiContextHolder {

    private static final ThreadLocal<ApiContext> CTX = new ThreadLocal<>();

    private ApiContextHolder() {
    }

    public static void set(ApiContext context) {
        CTX.set(context);
    }

    public static ApiContext get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }
}

