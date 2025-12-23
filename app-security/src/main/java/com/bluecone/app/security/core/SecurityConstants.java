package com.bluecone.app.security.core;

/**
 * 安全相关常量。
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * 无需登录即可访问的路径列表。
     * 
     * 注意：此列表应该只包含必要的公开接口，不要泛开放行。
     * - /api/admin/** 已移除，管理接口需要登录
     * - 微信回调接口需要放行（微信服务器无法携带登录 token）
     * - 用户登录/注册接口需要放行
     */
    public static final String[] PERMIT_ALL_PATHS = {
            "/",
            "/website/**",
            "/api/dev/**",
            "/ops/**",
            "/test/id/**",
            "/api/auth/**",                              // 认证接口（登录/刷新 token）
            "/api/gw/auth/**",                           // 网关认证接口
            "/api/user/auth/wechat-miniapp/**",          // 微信小程序登录接口
            "/api/tenants",                              // 租户创建接口（注册）
            "/actuator/health",                          // 健康检查接口
            "/api/wechat/open/callback/**",              // 微信开放平台回调接口（ticket 推送）
            "/api/wechat/open/auth/callback",            // 微信开放平台授权回调接口
            "/error"                                     // 错误页面
    };
}
