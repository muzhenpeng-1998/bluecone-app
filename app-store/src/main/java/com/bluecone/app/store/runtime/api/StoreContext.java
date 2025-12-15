package com.bluecone.app.store.runtime.api;

/**
 * @deprecated Moved to com.bluecone.app.core.store.StoreContext to avoid circular dependencies.
 * This type alias is kept for backward compatibility.
 */
@Deprecated
public record StoreContext(
        long tenantId,
        com.bluecone.app.id.core.Ulid128 storeInternalId,
        String storePublicId,
        com.bluecone.app.core.store.StoreSnapshot snapshot
) {
}

