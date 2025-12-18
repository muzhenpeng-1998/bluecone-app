package com.bluecone.app.store.application.service;

/**
 * 门店缓存失效器接口。
 * <p>职责：统一管理门店相关的缓存失效逻辑，后续可接入 L1/L2 缓存。</p>
 * <p>当前实现：空实现，预留扩展点，后续可接入多级缓存失效策略。</p>
 * <p>使用场景：在门店写操作成功后调用，确保缓存与数据库一致性。</p>
 */
public interface StoreCacheInvalidator {

    /**
     * 失效门店基础信息缓存。
     * <p>失效范围：门店基础信息（StoreBaseView）相关的缓存。</p>
     *
     * @param tenantId 租户 ID
     * @param storeId  门店 ID
     */
    default void invalidateStoreBase(Long tenantId, Long storeId) {
        // 当前为空实现，后续可接入 L1/L2 缓存失效逻辑
    }

    /**
     * 失效门店配置快照缓存。
     * <p>失效范围：门店完整配置（StoreConfig）相关的缓存，包括能力、营业时间等。</p>
     *
     * @param tenantId      租户 ID
     * @param storeId       门店 ID
     * @param configVersion 配置版本号（用于版本化缓存）
     */
    default void invalidateStoreConfig(Long tenantId, Long storeId, Long configVersion) {
        // 当前为空实现，后续可接入 L1/L2 缓存失效逻辑
    }

    /**
     * 失效门店订单快照缓存。
     * <p>失效范围：门店订单快照（StoreOrderSnapshot）相关的缓存，可能按渠道区分。</p>
     *
     * @param tenantId  租户 ID
     * @param storeId   门店 ID
     * @param channelType 渠道类型（可选，为 null 时失效所有渠道的缓存）
     */
    default void invalidateOrderSnapshot(Long tenantId, Long storeId, String channelType) {
        // 当前为空实现，后续可接入 L1/L2 缓存失效逻辑
    }
}
