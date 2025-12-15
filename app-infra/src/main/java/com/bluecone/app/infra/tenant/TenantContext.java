package com.bluecone.app.infra.tenant;

/**
 * @deprecated Moved to com.bluecone.app.core.tenant.TenantContext to avoid circular dependencies.
 * This class is kept for backward compatibility and delegates to the new location.
 */
@Deprecated
public class TenantContext {

    /**
     * 设置当前线程的租户 ID
     *
     * @param tenantId 租户 ID（通常为租户的唯一标识符）
     */
    public static void setTenantId(String tenantId) {
        com.bluecone.app.core.tenant.TenantContext.setTenantId(tenantId);
    }

    /**
     * 获取当前线程的租户 ID
     *
     * @return 租户 ID，如果未设置则返回 null
     */
    public static String getTenantId() {
        return com.bluecone.app.core.tenant.TenantContext.getTenantId();
    }

    /**
     * 清除当前线程的租户 ID
     *
     * 重要：必须在请求结束时调用此方法，避免线程池复用导致的租户数据泄露
     * 建议在 Filter 或 Interceptor 的 finally 块中调用
     */
    public static void clear() {
        com.bluecone.app.core.tenant.TenantContext.clear();
    }
}
