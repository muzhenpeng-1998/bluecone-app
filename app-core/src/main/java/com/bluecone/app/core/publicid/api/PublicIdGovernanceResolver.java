package com.bluecone.app.core.publicid.api;

import com.bluecone.app.id.api.ResourceType;

import java.util.List;
import java.util.Map;

/**
 * Public ID 治理解析器，提供统一的 publicId -> 内部主键的映射能力。
 * 
 * <p>核心职责：</p>
 * <ul>
 *   <li>格式校验：调用 IdService.validatePublicId() 校验前缀和格式</li>
 *   <li>查找映射：聚合多个 PublicIdLookup，根据资源类型路由查询</li>
 *   <li>批量支持：提供 resolveBatch 避免 N+1 查询</li>
 *   <li>异常统一：校验失败抛 PublicIdInvalidException，未找到抛 PublicIdNotFoundException</li>
 * </ul>
 * 
 * <p>与现有 PublicIdResolver 的区别：</p>
 * <ul>
 *   <li>PublicIdResolver：基于映射表 + 缓存，返回 Ulid128</li>
 *   <li>PublicIdGovernanceResolver：直接查业务表，返回 Long 主键，适用于 Controller 层强制治理</li>
 * </ul>
 */
public interface PublicIdGovernanceResolver {

    /**
     * 解析单个 publicId，返回完整的解析结果。
     * 
     * <p>执行流程：</p>
     * <ol>
     *   <li>校验 publicId 格式和前缀（调用 IdService.validatePublicId）</li>
     *   <li>根据 ResourceType 查找对应的 PublicIdLookup</li>
     *   <li>调用 Lookup.findInternalId 查询内部主键</li>
     *   <li>构造 ResolvedPublicId 返回</li>
     * </ol>
     * 
     * @param tenantId 租户 ID
     * @param type 资源类型
     * @param publicId 对外公开 ID
     * @return 解析结果
     * @throws com.bluecone.app.core.publicid.exception.PublicIdInvalidException 格式非法或前缀不匹配
     * @throws com.bluecone.app.core.publicid.exception.PublicIdNotFoundException 未找到对应资源
     * @throws com.bluecone.app.core.publicid.exception.PublicIdLookupMissingException 缺少对应的 Lookup 实现
     */
    ResolvedPublicId resolve(long tenantId, ResourceType type, String publicId);

    /**
     * 批量解析 publicId，避免 N+1 查询。
     * 
     * <p>使用场景：</p>
     * <ul>
     *   <li>列表接口：批量查询门店/商品详情</li>
     *   <li>关联查询：订单关联的多个 SKU</li>
     * </ul>
     * 
     * <p>实现要求：</p>
     * <ul>
     *   <li>先批量校验所有 publicId 格式</li>
     *   <li>调用 Lookup.findInternalIds 批量查询</li>
     *   <li>任一 publicId 未找到时，默认抛 PublicIdNotFoundException（可配置为返回部分结果）</li>
     * </ul>
     * 
     * @param tenantId 租户 ID
     * @param type 资源类型
     * @param publicIds 对外公开 ID 列表
     * @return publicId -> ResolvedPublicId 的映射
     * @throws com.bluecone.app.core.publicid.exception.PublicIdInvalidException 任一格式非法
     * @throws com.bluecone.app.core.publicid.exception.PublicIdNotFoundException 任一未找到
     * @throws com.bluecone.app.core.publicid.exception.PublicIdLookupMissingException 缺少对应的 Lookup 实现
     */
    Map<String, ResolvedPublicId> resolveBatch(long tenantId, ResourceType type, List<String> publicIds);
}

