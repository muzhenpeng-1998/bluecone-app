package com.bluecone.app.store.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.entity.BcStoreCapability;
import com.bluecone.app.store.dao.entity.BcStoreChannel;
import com.bluecone.app.store.dao.entity.BcStoreDevice;
import com.bluecone.app.store.dao.entity.BcStoreOpeningHours;
import com.bluecone.app.store.dao.entity.BcStorePrintRule;
import com.bluecone.app.store.dao.entity.BcStoreResource;
import com.bluecone.app.store.dao.entity.BcStoreSpecialDay;
import com.bluecone.app.store.dao.entity.BcStoreStaff;
import com.bluecone.app.store.dao.mapper.BcStoreCapabilityMapper;
import com.bluecone.app.store.dao.mapper.BcStoreChannelMapper;
import com.bluecone.app.store.dao.mapper.BcStoreDeviceMapper;
import com.bluecone.app.store.dao.mapper.BcStoreMapper;
import com.bluecone.app.store.dao.mapper.BcStoreOpeningHoursMapper;
import com.bluecone.app.store.dao.mapper.BcStorePrintRuleMapper;
import com.bluecone.app.store.dao.mapper.BcStoreResourceMapper;
import com.bluecone.app.store.dao.mapper.BcStoreSpecialDayMapper;
import com.bluecone.app.store.dao.mapper.BcStoreStaffMapper;
import com.bluecone.app.store.domain.exception.StoreConfigVersionConflictException;
import com.bluecone.app.store.domain.model.StoreCapabilityModel;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.model.StoreOpeningSchedule;
import com.bluecone.app.store.domain.repository.StoreRepository;
import com.bluecone.app.store.infrastructure.assembler.StoreConfigAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 门店仓储实现，基于已有 Mapper 从多张表组装 StoreConfig。
 * <p>当前直接访问 DB，后续由上层 StoreContextProvider 增加本地缓存/Redis 缓存包装以支撑高并发。</p>
 * <p>高隔离：对领域层提供语义化方法，隐藏 ORM 与表结构；高稳定：关键查询/更新均可被缓存与乐观锁保护；高并发：一次性聚合多表形成快照，减少链路内多次 IO。</p>
 */
@Repository
@RequiredArgsConstructor
public class StoreRepositoryImpl implements StoreRepository {

    private final BcStoreMapper bcStoreMapper;
    private final BcStoreCapabilityMapper bcStoreCapabilityMapper;
    private final BcStoreOpeningHoursMapper bcStoreOpeningHoursMapper;
    private final BcStoreSpecialDayMapper bcStoreSpecialDayMapper;
    private final BcStoreChannelMapper bcStoreChannelMapper;
    private final BcStoreResourceMapper bcStoreResourceMapper;
    private final BcStoreDeviceMapper bcStoreDeviceMapper;
    private final BcStorePrintRuleMapper bcStorePrintRuleMapper;
    private final BcStoreStaffMapper bcStoreStaffMapper;
    private final StoreConfigAssembler storeConfigAssembler;

    @Override
    public StoreConfig loadFullConfig(Long tenantId, Long storeId) {
        // 1）查主档 bc_store（限定租户 + 门店 + 未删除）
        BcStore store = bcStoreMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false));
        if (store == null) {
            return null;
        }

        // 2）查询能力配置
        List<BcStoreCapability> capabilities = bcStoreCapabilityMapper.selectList(new LambdaQueryWrapper<BcStoreCapability>()
                .eq(BcStoreCapability::getTenantId, tenantId)
                .eq(BcStoreCapability::getStoreId, storeId)
                .eq(BcStoreCapability::getIsDeleted, false));
        // 3）查询常规营业时间
        List<BcStoreOpeningHours> openingHours = bcStoreOpeningHoursMapper.selectList(new LambdaQueryWrapper<BcStoreOpeningHours>()
                .eq(BcStoreOpeningHours::getTenantId, tenantId)
                .eq(BcStoreOpeningHours::getStoreId, storeId)
                .eq(BcStoreOpeningHours::getIsDeleted, false));
        // 4）查询特殊日
        List<BcStoreSpecialDay> specialDays = bcStoreSpecialDayMapper.selectList(new LambdaQueryWrapper<BcStoreSpecialDay>()
                .eq(BcStoreSpecialDay::getTenantId, tenantId)
                .eq(BcStoreSpecialDay::getStoreId, storeId)
                .eq(BcStoreSpecialDay::getIsDeleted, false));
        // 预留其他配置（渠道/资源/设备/打印/员工）
        List<BcStoreChannel> channels = bcStoreChannelMapper.selectList(new LambdaQueryWrapper<BcStoreChannel>()
                .eq(BcStoreChannel::getTenantId, tenantId)
                .eq(BcStoreChannel::getStoreId, storeId)
                .eq(BcStoreChannel::getIsDeleted, false));
        List<BcStoreResource> resources = bcStoreResourceMapper.selectList(new LambdaQueryWrapper<BcStoreResource>()
                .eq(BcStoreResource::getTenantId, tenantId)
                .eq(BcStoreResource::getStoreId, storeId)
                .eq(BcStoreResource::getIsDeleted, false));
        List<BcStoreDevice> devices = bcStoreDeviceMapper.selectList(new LambdaQueryWrapper<BcStoreDevice>()
                .eq(BcStoreDevice::getTenantId, tenantId)
                .eq(BcStoreDevice::getStoreId, storeId)
                .eq(BcStoreDevice::getIsDeleted, false));
        List<BcStorePrintRule> printRules = bcStorePrintRuleMapper.selectList(new LambdaQueryWrapper<BcStorePrintRule>()
                .eq(BcStorePrintRule::getTenantId, tenantId)
                .eq(BcStorePrintRule::getStoreId, storeId)
                .eq(BcStorePrintRule::getIsDeleted, false));
        List<BcStoreStaff> staff = bcStoreStaffMapper.selectList(new LambdaQueryWrapper<BcStoreStaff>()
                .eq(BcStoreStaff::getTenantId, tenantId)
                .eq(BcStoreStaff::getStoreId, storeId)
                .eq(BcStoreStaff::getIsDeleted, false));

        // 汇总装配为领域聚合 StoreConfig，便于作为快照直接缓存
        return storeConfigAssembler.assembleStoreConfig(
                store,
                capabilities,
                openingHours,
                specialDays,
                channels,
                resources,
                devices,
                printRules,
                staff
        );
    }

    @Override
    public long getConfigVersion(Long tenantId, Long storeId) {
        // 只查版本号，避免不必要的列加载
        BcStore store = bcStoreMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .select(BcStore::getConfigVersion)
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false));
        // 高并发下此查询可结合本地缓存或批量预取优化
        return store != null && store.getConfigVersion() != null ? store.getConfigVersion() : 0L;
    }

    @Override
    public long bumpConfigVersion(Long tenantId, Long storeId, long expectedOldVersion) {
        // 乐观锁：WHERE config_version = expectedOldVersion
        LambdaUpdateWrapper<BcStore> updateWrapper = new LambdaUpdateWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getConfigVersion, expectedOldVersion)
                .set(BcStore::getConfigVersion, expectedOldVersion + 1);
        int updated = bcStoreMapper.update(null, updateWrapper);
        if (updated == 0) {
            // 未命中乐观锁，说明有并发写入，应让上层重试或提示刷新
            throw new StoreConfigVersionConflictException("configVersion 冲突，tenantId=" + tenantId + ", storeId=" + storeId);
        }
        return expectedOldVersion + 1;
    }

    @Override
    public void updateOpeningSchedule(Long tenantId, Long storeId, StoreOpeningSchedule schedule) {
        // TODO: 补充具体更新逻辑（删除旧配置 + 批量插入），并结合 configVersion 保护并发；完成后需要刷新缓存
        throw new UnsupportedOperationException("TODO implement updateOpeningSchedule");
    }

    @Override
    public void updateCapabilities(Long tenantId, Long storeId, Iterable<StoreCapabilityModel> capabilities) {
        // TODO: 补充具体更新逻辑（批量 upsert），并结合 configVersion 保护并发；完成后需要刷新缓存
        throw new UnsupportedOperationException("TODO implement updateCapabilities");
    }
}
