package com.bluecone.app.core.publicid.guard;

import com.bluecone.app.core.apicontract.ApiSide;

/**
 * Scope Guard 校验上下文，携带当前请求的权限范围信息。
 * 
 * <p>数据来源：</p>
 * <ul>
 *   <li>tenantId：从 ApiContext 或 TenantContext 获取</li>
 *   <li>storePk：从 ApiContext 的 STORE_CONTEXT 属性获取（如已解析）</li>
 *   <li>apiSide：从 RouteContract 解析，判断是 USER/MERCHANT/PLATFORM</li>
 * </ul>
 * 
 * @param tenantId 当前请求的租户 ID
 * @param storePk 当前请求的门店主键（可选，仅门店相关接口有值）
 * @param apiSide API 侧别（USER/MERCHANT/PLATFORM）
 */
public record ScopeGuardContext(
        long tenantId,
        Long storePk,
        ApiSide apiSide
) {
    
    /**
     * 判断是否为平台侧请求。
     * 
     * @return true 表示平台侧，false 表示用户侧或商户侧
     */
    public boolean isPlatformSide() {
        return apiSide == ApiSide.PLATFORM;
    }
    
    /**
     * 判断是否有门店上下文。
     * 
     * @return true 表示当前请求已绑定门店
     */
    public boolean hasStoreContext() {
        return storePk != null;
    }
}

