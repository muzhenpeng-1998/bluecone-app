package com.bluecone.app.order.infra.cache;

/**
 * 订单草稿 / 购物车 缓存键规范。
 */
public final class OrderDraftCacheKeys {

    private static final String PREFIX = "order:draft";

    private OrderDraftCacheKeys() {
    }

    /**
     * 构建草稿缓存键：order:draft:tenant:store:user:channel:scene
     */
    public static String buildDraftKey(Long tenantId,
                                       Long storeId,
                                       Long userId,
                                       String channel,
                                       String scene) {
        return String.join(":",
                PREFIX,
                safe(tenantId),
                safe(storeId),
                safe(userId),
                safe(channel),
                safe(scene));
    }

    private static String safe(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
