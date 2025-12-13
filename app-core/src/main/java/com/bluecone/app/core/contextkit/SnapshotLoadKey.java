package com.bluecone.app.core.contextkit;

/**
 * 快照加载键：聚合租户、scopeType 与 scopeId。
 *
 * scopeType 示例：STORE/PRODUCT/INVENTORY/USER 等或具体 namespace（如 store:snap）。
 */
public record SnapshotLoadKey(long tenantId, String scopeType, Object scopeId) {
}

