package com.bluecone.app.product.runtime.api;

import com.bluecone.app.id.core.Ulid128;

import java.time.Instant;

/**
 * 商品轻量快照，仅包含下单确认所需字段。
 */
public record ProductSnapshot(
        Ulid128 productId,
        String productPublicId,
        long tenantId,
        int status,
        long configVersion,
        String name,
        String coverImg,
        Instant updatedAt
) {
}

