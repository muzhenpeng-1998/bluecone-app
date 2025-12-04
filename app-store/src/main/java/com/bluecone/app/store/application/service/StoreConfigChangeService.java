package com.bluecone.app.store.application.service;

/**
 * 店铺配置变更通知服务。
 * <p>所有会影响门店配置快照（StoreConfig）的写操作成功后，必须调用本服务。</p>
 * <p>当前仅预留扩展点，后续可在此接入缓存失效、多级缓存刷新、领域事件等逻辑。</p>
 */
public interface StoreConfigChangeService {

    /**
     * 在门店配置成功变更之后触发。
     *
     * @param tenantId          租户 ID
     * @param storeId           门店 ID
     * @param newConfigVersion  最新的配置版本号
     */
    void onStoreConfigChanged(Long tenantId, Long storeId, Long newConfigVersion);
}
