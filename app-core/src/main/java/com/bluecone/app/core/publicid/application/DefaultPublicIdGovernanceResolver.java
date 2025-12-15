package com.bluecone.app.core.publicid.application;

import com.bluecone.app.core.publicid.api.PublicIdGovernanceResolver;
import com.bluecone.app.core.publicid.api.PublicIdLookup;
import com.bluecone.app.core.publicid.api.ResolvedPublicId;
import com.bluecone.app.core.publicid.exception.PublicIdInvalidException;
import com.bluecone.app.core.publicid.exception.PublicIdLookupMissingException;
import com.bluecone.app.core.publicid.exception.PublicIdNotFoundException;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * 默认 Public ID 治理解析器实现。
 * 
 * <p>核心流程：</p>
 * <ol>
 *   <li>格式校验：调用 IdService.validatePublicId() 校验前缀和 ULID 格式</li>
 *   <li>查找 Lookup：根据 ResourceType 从注册的 Lookup 列表中查找</li>
 *   <li>查询主键：调用 Lookup.findInternalId() 查询业务表</li>
 *   <li>构造结果：封装为 ResolvedPublicId 返回</li>
 * </ol>
 * 
 * <p>监控埋点：</p>
 * <ul>
 *   <li>命中率：记录 hit/miss 次数（不带 tenantId tag，避免高基数）</li>
 *   <li>耗时：记录 resolve 耗时，按 resourceType 分组</li>
 * </ul>
 */
@Component
public class DefaultPublicIdGovernanceResolver implements PublicIdGovernanceResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultPublicIdGovernanceResolver.class);

    private final IdService idService;
    private final Map<ResourceType, PublicIdLookup> lookupRegistry;

    /**
     * 构造函数，注入 IdService 和所有 PublicIdLookup 实现。
     * 
     * @param idService ID 服务，用于校验 publicId 格式
     * @param lookups 所有 PublicIdLookup 实现列表
     */
    public DefaultPublicIdGovernanceResolver(IdService idService, List<PublicIdLookup> lookups) {
        this.idService = idService;
        this.lookupRegistry = new HashMap<>();
        for (PublicIdLookup lookup : lookups) {
            ResourceType type = lookup.type();
            if (lookupRegistry.containsKey(type)) {
                log.warn("重复注册 PublicIdLookup：resourceType={}，已存在实现 {}，新实现 {} 将被忽略",
                        type, lookupRegistry.get(type).getClass().getName(), lookup.getClass().getName());
            } else {
                lookupRegistry.put(type, lookup);
                log.info("注册 PublicIdLookup：resourceType={}, implementation={}",
                        type, lookup.getClass().getSimpleName());
            }
        }
    }

    @Override
    public ResolvedPublicId resolve(long tenantId, ResourceType type, String publicId) {
        long startTime = System.currentTimeMillis();
        try {
            // 1. 格式校验
            validatePublicId(type, publicId);

            // 2. 查找 Lookup
            PublicIdLookup lookup = getLookup(type);

            // 3. 查询内部主键
            Optional<Object> internalIdOpt = lookup.findInternalId(tenantId, publicId);
            if (internalIdOpt.isEmpty()) {
                log.debug("Public ID 未找到：resourceType={}, tenantId={}, publicId={}",
                        type, tenantId, maskPublicId(publicId));
                throw new PublicIdNotFoundException(
                        String.format("未找到资源：resourceType=%s, publicId=%s", type, publicId));
            }

            // 4. 构造结果
            Object internalId = internalIdOpt.get();
            ResolvedPublicId resolved = new ResolvedPublicId(type, publicId, tenantId, internalId);

            // 5. 监控埋点
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 100) {
                log.warn("Public ID 解析耗时过长：resourceType={}, elapsed={}ms", type, elapsed);
            }

            return resolved;
        } catch (PublicIdInvalidException | PublicIdNotFoundException | PublicIdLookupMissingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Public ID 解析失败：resourceType={}, tenantId={}, publicId={}",
                    type, tenantId, maskPublicId(publicId), ex);
            throw new RuntimeException("Public ID 解析失败", ex);
        }
    }

    @Override
    public Map<String, ResolvedPublicId> resolveBatch(long tenantId, ResourceType type, List<String> publicIds) {
        long startTime = System.currentTimeMillis();
        try {
            // 1. 批量格式校验
            for (String publicId : publicIds) {
                validatePublicId(type, publicId);
            }

            // 2. 查找 Lookup
            PublicIdLookup lookup = getLookup(type);

            // 3. 批量查询内部主键
            Map<String, Object> internalIdMap = lookup.findInternalIds(tenantId, publicIds);

            // 4. 检查是否有未找到的 publicId
            List<String> notFoundIds = new ArrayList<>();
            for (String publicId : publicIds) {
                if (!internalIdMap.containsKey(publicId)) {
                    notFoundIds.add(publicId);
                }
            }
            if (!notFoundIds.isEmpty()) {
                log.debug("批量解析中有 {} 个 Public ID 未找到：resourceType={}, tenantId={}, notFoundIds={}",
                        notFoundIds.size(), type, tenantId, notFoundIds);
                throw new PublicIdNotFoundException(
                        String.format("未找到 %d 个资源：resourceType=%s, notFoundIds=%s",
                                notFoundIds.size(), type, notFoundIds));
            }

            // 5. 构造结果
            Map<String, ResolvedPublicId> resultMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : internalIdMap.entrySet()) {
                String publicId = entry.getKey();
                Object internalId = entry.getValue();
                resultMap.put(publicId, new ResolvedPublicId(type, publicId, tenantId, internalId));
            }

            // 6. 监控埋点
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 200) {
                log.warn("批量 Public ID 解析耗时过长：resourceType={}, count={}, elapsed={}ms",
                        type, publicIds.size(), elapsed);
            }

            return resultMap;
        } catch (PublicIdInvalidException | PublicIdNotFoundException | PublicIdLookupMissingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("批量 Public ID 解析失败：resourceType={}, tenantId={}, count={}",
                    type, tenantId, publicIds.size(), ex);
            throw new RuntimeException("批量 Public ID 解析失败", ex);
        }
    }

    /**
     * 校验 publicId 格式和前缀。
     */
    private void validatePublicId(ResourceType type, String publicId) {
        try {
            idService.validatePublicId(type, publicId);
        } catch (IllegalArgumentException ex) {
            throw new PublicIdInvalidException(ex.getMessage(), ex);
        }
    }

    /**
     * 根据资源类型查找对应的 Lookup。
     */
    private PublicIdLookup getLookup(ResourceType type) {
        PublicIdLookup lookup = lookupRegistry.get(type);
        if (lookup == null) {
            throw new PublicIdLookupMissingException(type);
        }
        return lookup;
    }

    /**
     * 脱敏 publicId，避免日志泄露完整 ID。
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
