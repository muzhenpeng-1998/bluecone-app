package com.bluecone.app.store.domain.repository;

import com.bluecone.app.store.domain.model.StoreCapabilityModel;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.model.StoreOpeningSchedule;
import com.bluecone.app.store.domain.model.runtime.StoreRuntime;

import java.util.Optional;

/**
 * 领域仓储接口，屏蔽 MyBatis-Plus 细节，向领域层提供语义化的持久化能力。
 */
public interface StoreRepository {

    /**
     * 从多张表加载完整门店配置并组装为 StoreConfig。
     * <p>当前版本直接查 DB，后续由上层通过缓存包装以应对高并发。</p>
     */
    StoreConfig loadFullConfig(Long tenantId, Long storeId);

    /**
     * 仅查询 config_version 字段，高并发下可结合本地缓存优化。
     */
    long getConfigVersion(Long tenantId, Long storeId);

    /**
     * 使用乐观锁更新 config_version，避免并发覆盖。
     *
     * @param tenantId           租户 ID
     * @param storeId            门店 ID
     * @param expectedOldVersion 期望的旧版本
     * @return 更新后的新版本；若更新失败，返回约定的特殊值或抛出异常（后续补齐）
     */
    long bumpConfigVersion(Long tenantId, Long storeId, long expectedOldVersion);

    /**
     * 更新营业时间配置（预留，后续补充具体实现）。
     */
    void updateOpeningSchedule(Long tenantId, Long storeId, StoreOpeningSchedule schedule);

    /**
     * 更新能力列表配置（预留，后续补充具体实现）。
     */
    void updateCapabilities(Long tenantId, Long storeId, Iterable<StoreCapabilityModel> capabilities);

    /**
     * 加载门店运行时快照，供下游订单/菜单等模块消费。
     *
     * @param tenantId 租户 ID
     * @param storeId  门店 ID
     * @return 门店运行时快照（Optional 包装）
     */
    Optional<StoreRuntime> loadStoreRuntime(Long tenantId, Long storeId);
}
