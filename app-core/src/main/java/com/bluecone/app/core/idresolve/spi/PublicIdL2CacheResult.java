package com.bluecone.app.core.idresolve.spi;

import com.bluecone.app.id.core.Ulid128;

/**
 * L2 缓存查询结果。
 *
 * @param hit        是否命中 L2
 * @param negative   是否为负缓存命中
 * @param internalId 命中的内部 ID（仅正向命中时非空）
 */
public record PublicIdL2CacheResult(boolean hit, boolean negative, Ulid128 internalId) {

    public static PublicIdL2CacheResult miss() {
        return new PublicIdL2CacheResult(false, false, null);
    }

    public static PublicIdL2CacheResult negativeHit() {
        return new PublicIdL2CacheResult(true, true, null);
    }

    public static PublicIdL2CacheResult positiveHit(Ulid128 id) {
        return new PublicIdL2CacheResult(true, false, id);
    }
}

