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
     * <p>聚合门店主表、能力配置、营业时间、特殊日、渠道、资源、设备、打印规则、员工等多张表的数据，
     * 组装为领域聚合根 StoreConfig，便于作为快照直接缓存。</p>
     * <p>当前版本直接查 DB，后续由上层通过缓存包装以应对高并发。</p>
     *
     * @param tenantId 租户 ID
     * @param storeId  门店 ID
     * @return 门店完整配置聚合（如门店不存在则返回 null）
     */
    StoreConfig loadFullConfig(Long tenantId, Long storeId);

    /**
     * 仅查询 config_version 字段，高并发下可结合本地缓存优化。
     * <p>说明：此方法仅查询版本号，避免不必要的列加载，适用于高并发场景下的版本校验。</p>
     *
     * @param tenantId 租户 ID
     * @param storeId  门店 ID
     * @return 配置版本号（如门店不存在则返回 0）
     */
    long getConfigVersion(Long tenantId, Long storeId);

    /**
     * 使用乐观锁更新 config_version，避免并发覆盖。
     * <p>说明：此方法使用乐观锁机制，只有当 config_version 等于 expectedOldVersion 时才会更新为 expectedOldVersion + 1。
     * 若版本不匹配（说明有并发写入），则抛出 StoreConfigVersionConflictException，调用方需重试或提示刷新。</p>
     *
     * @param tenantId           租户 ID
     * @param storeId            门店 ID
     * @param expectedOldVersion 期望的旧版本（乐观锁条件）
     * @return 更新后的新版本（expectedOldVersion + 1）
     * @throws com.bluecone.app.store.domain.exception.StoreConfigVersionConflictException 当版本不匹配时抛出
     */
    long bumpConfigVersion(Long tenantId, Long storeId, long expectedOldVersion);

    /**
     * 更新门店营业时间配置（常规营业时间 + 特殊日）。
     * <p>实现策略：采用"先删后插"的幂等策略，先软删该门店所有常规营业时间和特殊日记录，再批量插入新配置。</p>
     * <p>说明：configVersion 的校验和递增由上层应用服务负责，此方法仅负责数据持久化。</p>
     *
     * @param tenantId 租户 ID
     * @param storeId  门店 ID
     * @param schedule 营业时间配置聚合（包含常规营业时间和特殊日）
     */
    void updateOpeningSchedule(Long tenantId, Long storeId, StoreOpeningSchedule schedule);

    /**
     * 更新门店能力配置列表。
     * <p>实现策略：采用"先删后插"的幂等策略，先软删该门店所有能力记录，再批量插入新配置。</p>
     * <p>说明：configVersion 的校验和递增由上层应用服务负责，此方法仅负责数据持久化。</p>
     *
     * @param tenantId    租户 ID
     * @param storeId     门店 ID
     * @param capabilities 能力配置列表（如 DINE_IN、TAKE_OUT、PICKUP 等）
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
