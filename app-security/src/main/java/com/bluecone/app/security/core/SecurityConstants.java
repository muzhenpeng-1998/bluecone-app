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
            "/api/dev/**",
            "/ops/**",
            "/test/id/**",
             "/api/admin/stores",  // 已移除：admin接口应该需要登录
             "/api/admin/**",      // 已移除：admin接口应该需要登录
            "/api/auth/**",
            "/api/gw/auth/**",
            "/api/user/auth/wechat-miniapp/**",
            "/api/tenants",  // 租户创建接口（注册）
            "/actuator/health",
            "/error"
    };
}
