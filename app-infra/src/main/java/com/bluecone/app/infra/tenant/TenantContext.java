package com.bluecone.app.infra.tenant;

/**
 * 租户上下文管理器
 *
 * 功能：
 * - 使用 ThreadLocal 存储当前线程的租户 ID
 * - 提供租户 ID 的设置、获取、清除操作
 *
 * 使用场景：
 * 1. 请求拦截器（RequestInterceptor）在请求开始时设置租户 ID
 * 2. MyBatis-Plus 租户拦截器（TenantLineHandler）在 SQL 执行时获取租户 ID
 * 3. 请求结束时清除租户 ID，避免线程池复用导致的数据泄露
 *
 * 注意事项：
 * - 必须在请求结束时调用 clear() 方法清理 ThreadLocal
 * - 租户 ID 通常从 HTTP Header、JWT Token 或 Session 中提取
 *
 * 未来集成点：
 * - app-security 模块的 JwtAuthenticationFilter 会负责填充租户 ID
 * - app-application 模块的 RequestInterceptor 会在请求结束时清理租户 ID
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
public class TenantContext {

    /**
     * 租户 ID 存储容器
     * 使用 ThreadLocal 确保线程隔离，避免多线程环境下的数据混乱
     */
    private static final ThreadLocal<String> TENANT_ID_HOLDER = ThreadLocal.withInitial(() -> null);

    /**
     * 设置当前线程的租户 ID
     *
     * @param tenantId 租户 ID（通常为租户的唯一标识符）
     */
    public static void setTenantId(String tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * 获取当前线程的租户 ID
     *
     * @return 租户 ID，如果未设置则返回 null
     */
    public static String getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 清除当前线程的租户 ID
     *
     * 重要：必须在请求结束时调用此方法，避免线程池复用导致的租户数据泄露
     * 建议在 Filter 或 Interceptor 的 finally 块中调用
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}
