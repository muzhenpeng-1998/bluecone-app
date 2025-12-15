package com.bluecone.app.core.publicid.api;

import com.bluecone.app.id.api.ResourceType;

/**
 * Public ID 解析结果，包含完整的上下文信息。
 * 
 * <p>用于 Controller 参数注入，提供：</p>
 * <ul>
 *   <li>资源类型：便于后续业务逻辑判断</li>
 *   <li>publicId：原始对外 ID，用于日志/审计</li>
 *   <li>tenantId：所属租户，用于权限校验</li>
 *   <li>internalIdOrPk：内部主键（Long）或 internal_id（Ulid128）</li>
 * </ul>
 * 
 * <p>使用示例：</p>
 * <pre>
 * &#64;GetMapping("/stores/{storeId}")
 * public StoreView detail(&#64;PathVariable &#64;ResolvePublicId(type=STORE) ResolvedPublicId resolved) {
 *     long storePk = (Long) resolved.internalIdOrPk();
 *     // 业务逻辑...
 * }
 * </pre>
 * 
 * @param type 资源类型
 * @param publicId 对外公开 ID
 * @param tenantId 所属租户 ID
 * @param internalIdOrPk 内部主键（Long）或 internal_id（Ulid128）
 */
public record ResolvedPublicId(
        ResourceType type,
        String publicId,
        long tenantId,
        Object internalIdOrPk
) {
    
    /**
     * 获取 Long 类型的主键（适用于自增主键场景）。
     * 
     * @return Long 主键
     * @throws ClassCastException 如果内部 ID 不是 Long 类型
     */
    public Long asLong() {
        return (Long) internalIdOrPk;
    }
    
    /**
     * 获取 Ulid128 类型的内部 ID（适用于分布式 ID 场景）。
     * 
     * @return Ulid128 内部 ID
     * @throws ClassCastException 如果内部 ID 不是 Ulid128 类型
     */
    public com.bluecone.app.id.core.Ulid128 asUlid() {
        return (com.bluecone.app.id.core.Ulid128) internalIdOrPk;
    }
}

