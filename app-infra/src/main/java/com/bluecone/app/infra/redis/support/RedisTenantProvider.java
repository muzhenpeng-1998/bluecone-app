package com.bluecone.app.infra.redis.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.infra.tenant.TenantContext;

/**
 * 提供 Redis key 前缀用的租户标识。
 * 仅用于 key 命名隔离，不负责权限或鉴权逻辑。
 */
@Component
public class RedisTenantProvider {

    private static final long DEFAULT_TENANT_ID = 0L;

    /**
     * 从 {@link TenantContext} 读取当前租户 ID，不存在或非法时返回 0。
     *
     * @return 用于 Redis key 前缀的租户 ID
     */
    public Long getTenantIdOrDefault() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return DEFAULT_TENANT_ID;
        }
        try {
            return Long.parseLong(tenantId.trim());
        } catch (NumberFormatException ignored) {
            return DEFAULT_TENANT_ID;
        }
    }
}
