package com.bluecone.app.core.publicid.guard;

import com.bluecone.app.core.publicid.api.ResolvedPublicId;

/**
 * Scope Guard 接口，用于防止 Public ID 越权访问。
 * 
 * <p>核心职责：</p>
 * <ul>
 *   <li>租户隔离：确保 resolved.tenantId 与上下文 tenantId 一致</li>
 *   <li>门店隔离：确保 resolved.storeId 与上下文 storeId 一致（如适用）</li>
 *   <li>防枚举攻击：避免通过遍历 publicId 访问其他租户/门店资源</li>
 * </ul>
 * 
 * <p>使用场景：</p>
 * <ul>
 *   <li>USER/MERCHANT 侧：强制启用，防止越权</li>
 *   <li>PLATFORM 侧：可选启用，避免误伤后台管理功能</li>
 * </ul>
 * 
 * <p>实现策略：</p>
 * <ul>
 *   <li>默认策略：租户级校验（所有资源）+ 门店级校验（STORE 资源）</li>
 *   <li>自定义策略：可针对特定资源类型实现更细粒度的校验</li>
 * </ul>
 */
public interface ScopeGuard {

    /**
     * 校验解析后的 Public ID 是否在当前请求的权限范围内。
     * 
     * <p>校验失败时抛出 PublicIdForbiddenException，由全局异常处理器映射为 403。</p>
     * 
     * @param resolved 解析后的 Public ID
     * @param context 当前请求上下文（包含 tenantId、storeId、apiSide 等）
     * @throws com.bluecone.app.core.publicid.exception.PublicIdForbiddenException 权限校验失败
     */
    void check(ResolvedPublicId resolved, ScopeGuardContext context);
}

