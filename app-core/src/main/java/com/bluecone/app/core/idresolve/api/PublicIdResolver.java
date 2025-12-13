package com.bluecone.app.core.idresolve.api;

import java.util.List;
import java.util.Map;

import com.bluecone.app.id.api.ResourceType;

/**
 * 公共 ID 解析器，将对外 publicId 映射为内部 ULID128。
 */
public interface PublicIdResolver {

    /**
     * 解析单个 publicId。
     */
    ResolveResult resolve(ResolveKey key);

    /**
     * 批量解析 publicId。返回 Map 的 key 为 publicId 字符串。
     */
    Map<String, ResolveResult> resolveBatch(long tenantId, ResourceType type, List<String> publicIds);
}

