package com.bluecone.app.core.idresolve.spi;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;

/**
 * 公共 ID L2 缓存抽象，通常由 Redis 实现。
 */
public interface PublicIdL2Cache {

    PublicIdL2CacheResult get(long tenantId, ResourceType type, String publicId);

    Map<String, PublicIdL2CacheResult> getBatch(long tenantId, ResourceType type, List<String> publicIds);

    void putPositive(long tenantId, ResourceType type, String publicId, Ulid128 internalId, Duration ttl);

    void putNegative(long tenantId, ResourceType type, String publicId, Duration ttl);
}

