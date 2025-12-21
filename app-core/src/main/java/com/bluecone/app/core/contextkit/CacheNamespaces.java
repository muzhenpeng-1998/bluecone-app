package com.bluecone.app.core.contextkit;

/**
 * Common cache namespaces used across ContextMiddlewareKit and
 * cache invalidation events.
 */
public final class CacheNamespaces {

    private CacheNamespaces() {
    }

    public static final String STORE_SNAPSHOT = "store:snap";
    public static final String PRODUCT_SNAPSHOT = "product:snap";
    public static final String SKU_SNAPSHOT = "sku:snap";
    public static final String INVENTORY_POLICY = "inventory:policy";
    public static final String USER_SNAPSHOT = "user:snap";
    
    /**
     * 门店菜单快照缓存命名空间（Prompt 08）。
     * <p>
     * key 格式：{tenantId}:{epoch}:{storeId}:{channel}:{orderScene}
     */
    public static final String STORE_MENU_SNAPSHOT = "store:menu:snap";
}

