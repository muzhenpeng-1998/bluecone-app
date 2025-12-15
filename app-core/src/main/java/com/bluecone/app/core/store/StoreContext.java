package com.bluecone.app.core.store;

import com.bluecone.app.id.core.Ulid128;

/**
 * 门店上下文：聚合租户、门店标识以及快照信息。
 */
public record StoreContext(
        long tenantId,
        Ulid128 storeInternalId,
        String storePublicId,
        StoreSnapshot snapshot
) {
}

