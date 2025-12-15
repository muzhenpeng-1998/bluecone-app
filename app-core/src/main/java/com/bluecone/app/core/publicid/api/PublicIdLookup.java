package com.bluecone.app.core.publicid.api;

import com.bluecone.app.id.api.ResourceType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Public ID 查找 SPI，用于将 (tenantId, publicId) 映射为内部主键或 internal_id。
 * 
 * <p>各业务模块需实现此接口，提供从业务表直接查询 public_id 的能力。
 * 优先走 (tenant_id, public_id) 索引，避免引入额外映射表。</p>
 * 
 * <p>实现示例：</p>
 * <pre>
 * &#64;Component
 * public class StorePublicIdLookup implements PublicIdLookup {
 *     &#64;Override
 *     public ResourceType type() { return ResourceType.STORE; }
 *     
 *     &#64;Override
 *     public Optional&lt;Object&gt; findInternalId(long tenantId, String publicId) {
 *         // SELECT id FROM bc_store WHERE tenant_id=? AND public_id=? LIMIT 1
 *         return Optional.ofNullable(storeMapper.findIdByPublicId(tenantId, publicId));
 *     }
 * }
 * </pre>
 */
public interface PublicIdLookup {

    /**
     * 该 Lookup 负责的资源类型。
     * 
     * @return 资源类型枚举
     */
    ResourceType type();

    /**
     * 根据 tenantId 和 publicId 查找内部主键或 internal_id。
     * 
     * <p>返回类型说明：</p>
     * <ul>
     *   <li>Long：自增主键（推荐，性能最优）</li>
     *   <li>Ulid128：内部 ULID（适用于分布式场景）</li>
     * </ul>
     * 
     * <p>实现要求：</p>
     * <ul>
     *   <li>必须走 (tenant_id, public_id) 索引，避免全表扫描</li>
     *   <li>查询超时建议设置为 100ms 以内</li>
     *   <li>未找到时返回 Optional.empty()，不要抛异常</li>
     * </ul>
     * 
     * @param tenantId 租户 ID
     * @param publicId 对外公开 ID
     * @return 内部主键（Long）或 internal_id（Ulid128），未找到返回 empty
     */
    Optional<Object> findInternalId(long tenantId, String publicId);

    /**
     * 批量查找内部主键，避免 N+1 查询问题。
     * 
     * <p>实现建议：</p>
     * <ul>
     *   <li>使用 WHERE tenant_id=? AND public_id IN (?, ?, ...) 批量查询</li>
     *   <li>返回 Map 的 key 为 publicId，value 为内部主键</li>
     *   <li>未找到的 publicId 不出现在 Map 中（不要放 null）</li>
     *   <li>批量大小建议控制在 100 以内，超过则分批查询</li>
     * </ul>
     * 
     * @param tenantId 租户 ID
     * @param publicIds 对外公开 ID 列表
     * @return publicId -> 内部主键的映射，未找到的不包含在 Map 中
     */
    Map<String, Object> findInternalIds(long tenantId, List<String> publicIds);
}

