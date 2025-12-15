package com.bluecone.app.core.publicid.guard;

import com.bluecone.app.core.publicid.api.ResolvedPublicId;
import com.bluecone.app.core.publicid.exception.PublicIdForbiddenException;
import com.bluecone.app.id.api.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 默认 Scope Guard 实现，提供租户级和门店级的权限校验。
 * 
 * <p>校验策略：</p>
 * <ol>
 *   <li>平台侧（PLATFORM）：跳过所有校验（或仅做租户校验，可配置）</li>
 *   <li>租户校验：resolved.tenantId 必须等于 context.tenantId</li>
 *   <li>门店校验（仅 STORE 资源）：resolved.pk 必须等于 context.storePk</li>
 *   <li>其他资源（PRODUCT/SKU/ORDER）：仅做租户校验</li>
 * </ol>
 * 
 * <p>配置项：</p>
 * <ul>
 *   <li>skipPlatformSide：是否跳过平台侧校验（默认 true）</li>
 *   <li>enableStoreScope：是否启用门店级校验（默认 true）</li>
 * </ul>
 */
@Component
public class DefaultScopeGuard implements ScopeGuard {

    private static final Logger log = LoggerFactory.getLogger(DefaultScopeGuard.class);

    private final boolean skipPlatformSide;
    private final boolean enableStoreScope;

    /**
     * 使用默认配置构造（跳过平台侧，启用门店校验）。
     */
    public DefaultScopeGuard() {
        this(true, true);
    }

    /**
     * 自定义配置构造。
     * 
     * @param skipPlatformSide 是否跳过平台侧校验
     * @param enableStoreScope 是否启用门店级校验
     */
    public DefaultScopeGuard(boolean skipPlatformSide, boolean enableStoreScope) {
        this.skipPlatformSide = skipPlatformSide;
        this.enableStoreScope = enableStoreScope;
    }

    @Override
    public void check(ResolvedPublicId resolved, ScopeGuardContext context) {
        // 1. 平台侧跳过校验（可选）
        if (skipPlatformSide && context.isPlatformSide()) {
            log.debug("跳过平台侧 Scope Guard 校验：resourceType={}, publicId={}",
                    resolved.type(), maskPublicId(resolved.publicId()));
            return;
        }

        // 2. 租户隔离校验（必须）
        checkTenantScope(resolved, context);

        // 3. 门店隔离校验（仅 STORE 资源且上下文有门店时）
        if (enableStoreScope && resolved.type() == ResourceType.STORE && context.hasStoreContext()) {
            checkStoreScope(resolved, context);
        }

        // 4. 其他资源类型（PRODUCT/SKU/ORDER）暂时仅做租户校验
        // 后续可扩展更细粒度的校验（如商品归属门店校验）
    }

    /**
     * 租户隔离校验：resolved.tenantId 必须等于 context.tenantId。
     */
    private void checkTenantScope(ResolvedPublicId resolved, ScopeGuardContext context) {
        if (resolved.tenantId() != context.tenantId()) {
            // 安全提示：错误消息不暴露具体的 tenantId，避免信息泄露
            log.warn("租户隔离校验失败：resourceType={}, publicId={}, expectedTenant={}, actualTenant={}",
                    resolved.type(), maskPublicId(resolved.publicId()),
                    context.tenantId(), resolved.tenantId());
            throw new PublicIdForbiddenException("无权访问该资源");
        }
    }

    /**
     * 门店隔离校验：resolved.pk 必须等于 context.storePk。
     */
    private void checkStoreScope(ResolvedPublicId resolved, ScopeGuardContext context) {
        Long resolvedStorePk = (Long) resolved.internalIdOrPk();
        if (!resolvedStorePk.equals(context.storePk())) {
            // 安全提示：错误消息不暴露具体的 storeId，避免信息泄露
            log.warn("门店隔离校验失败：publicId={}, expectedStore={}, actualStore={}",
                    maskPublicId(resolved.publicId()), context.storePk(), resolvedStorePk);
            throw new PublicIdForbiddenException("无权访问该门店资源");
        }
    }

    /**
     * 脱敏 publicId，仅保留前缀和前 6 位，避免日志泄露完整 ID。
     * 
     * @param publicId 原始 publicId
     * @return 脱敏后的 publicId，例如 sto_01HN8X******
     */
    private String maskPublicId(String publicId) {
        if (publicId == null || publicId.length() < 10) {
            return publicId;
        }
        int separatorIndex = publicId.indexOf('_');
        if (separatorIndex < 0) {
            return publicId.substring(0, Math.min(6, publicId.length())) + "******";
        }
        String prefix = publicId.substring(0, separatorIndex + 1);
        String payload = publicId.substring(separatorIndex + 1);
        if (payload.length() <= 6) {
            return publicId;
        }
        return prefix + payload.substring(0, 6) + "******";
    }
}
