package com.bluecone.app.security.core;

/**
 * 安全相关常量。
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String[] PERMIT_ALL_PATHS = {
            "/",
            "/website/**",
            "/ops/**",
            "/api/admin/**",
            "/api/auth/**",
            "/api/gw/auth/**",
            "/actuator/health",
            "/error"
    };
}
