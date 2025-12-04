package com.bluecone.app.store.application.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.store.application.command.ChangeStoreStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreCommand;
import com.bluecone.app.store.application.command.ToggleOpenForOrdersCommand;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.application.command.UpdateStoreCapabilitiesCommand;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.store.application.command.UpdateStoreSpecialDaysCommand;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.entity.BcStoreCapability;
import com.bluecone.app.store.dao.entity.BcStoreOpeningHours;
import com.bluecone.app.store.dao.entity.BcStoreSpecialDay;
import com.bluecone.app.store.dao.service.IBcStoreCapabilityService;
import com.bluecone.app.store.dao.service.IBcStoreOpeningHoursService;
import com.bluecone.app.store.dao.service.IBcStoreService;
import com.bluecone.app.store.dao.service.IBcStoreSpecialDayService;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.domain.model.StoreCapabilityModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 店铺写侧应用服务。
 * <p>职责：封装所有对门店及配置的写操作，并统一做乐观锁控制与配置变更通知。</p>
 * <p>高隔离：上层只调用此服务，不直接操作 Mapper/Entity。</p>
 * <p>高稳定：使用 configVersion 乐观锁，写成功后触发配置变更钩子，便于后续缓存失效。</p>
 */
@Service
public class StoreCommandService {

    private static final Logger log = LoggerFactory.getLogger(StoreCommandService.class);

    private final IBcStoreService bcStoreService;
    private final IBcStoreCapabilityService bcStoreCapabilityService;
    private final IBcStoreOpeningHoursService bcStoreOpeningHoursService;
    private final IBcStoreSpecialDayService bcStoreSpecialDayService;
    private final StoreConfigChangeService storeConfigChangeService;

    public StoreCommandService(IBcStoreService bcStoreService,
                               IBcStoreCapabilityService bcStoreCapabilityService,
                               IBcStoreOpeningHoursService bcStoreOpeningHoursService,
                               IBcStoreSpecialDayService bcStoreSpecialDayService,
                               StoreConfigChangeService storeConfigChangeService) {
        this.bcStoreService = bcStoreService;
        this.bcStoreCapabilityService = bcStoreCapabilityService;
        this.bcStoreOpeningHoursService = bcStoreOpeningHoursService;
        this.bcStoreSpecialDayService = bcStoreSpecialDayService;
        this.storeConfigChangeService = storeConfigChangeService;
    }

    /**
     * 创建门店及默认配置。
     */
    @Transactional(rollbackFor = Exception.class)
    public void createStore(CreateStoreCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getName(), "门店名称不能为空");
        Objects.requireNonNull(command.getIndustryType(), "行业类型不能为空");

        Long tenantId = command.getTenantId();

        // 生成/校验门店编码
        String storeCode = command.getStoreCode();
        if (storeCode == null || storeCode.isBlank()) {
            storeCode = "S" + System.currentTimeMillis();
        } else {
            long cnt = bcStoreService.lambdaQuery()
                    .eq(BcStore::getTenantId, tenantId)
                    .eq(BcStore::getStoreCode, storeCode)
                    .eq(BcStore::getIsDeleted, false)
                    .count();
            if (cnt > 0) {
                throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "门店编码已存在");
            }
        }

        BcStore entity = new BcStore();
        entity.setTenantId(tenantId);
        entity.setStoreCode(storeCode);
        entity.setName(command.getName());
        entity.setShortName(command.getShortName());
        entity.setIndustryType(command.getIndustryType());
        entity.setCityCode(command.getCityCode());
        entity.setStatus("OPEN");
        entity.setOpenForOrders(Boolean.TRUE.equals(command.getOpenForOrders()));
        entity.setConfigVersion(1L);
        entity.setIsDeleted(false);
        // TODO 审计字段补充（createdAt/createdBy 等）

        bcStoreService.save(entity);

        Long storeId = entity.getId();
        // 默认能力/营业时间初始化留空，后续可补充

        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, entity.getConfigVersion());
        log.info("[createStore] tenantId={}, storeId={}, storeCode={}", tenantId, storeId, storeCode);
    }

    /**
     * 更新门店基础信息，使用 configVersion 乐观锁。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStoreBase(UpdateStoreBaseCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");

        Long tenantId = command.getTenantId();
        Long storeId = command.getStoreId();
        Long expectedVersion = command.getExpectedConfigVersion();

        LambdaUpdateWrapper<BcStore> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .eq(BcStore::getConfigVersion, expectedVersion);

        if (command.getName() != null) {
            wrapper.set(BcStore::getName, command.getName());
        }
        if (command.getShortName() != null) {
            wrapper.set(BcStore::getShortName, command.getShortName());
        }
        if (command.getIndustryType() != null) {
            wrapper.set(BcStore::getIndustryType, command.getIndustryType());
        }
        if (command.getCityCode() != null) {
            wrapper.set(BcStore::getCityCode, command.getCityCode());
        }
        if (command.getOpenForOrders() != null) {
            wrapper.set(BcStore::getOpenForOrders, command.getOpenForOrders());
        }

        wrapper.set(BcStore::getConfigVersion, expectedVersion + 1);

        boolean updated = bcStoreService.update(wrapper);
        if (!updated) {
            throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "更新门店基础信息失败，可能存在并发修改，请刷新后重试");
        }
        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, expectedVersion + 1);
    }

    /**
     * 批量更新能力配置：先删后插，再递增版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCapabilities(UpdateStoreCapabilitiesCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");

        Long tenantId = command.getTenantId();
        Long storeId = command.getStoreId();
        Long expectedVersion = command.getExpectedConfigVersion();

        bcStoreCapabilityService.lambdaUpdate()
                .eq(BcStoreCapability::getTenantId, tenantId)
                .eq(BcStoreCapability::getStoreId, storeId)
                .set(BcStoreCapability::getIsDeleted, true)
                .update();

        if (command.getCapabilities() != null) {
            for (StoreCapabilityModel item : command.getCapabilities()) {
                BcStoreCapability entity = new BcStoreCapability();
                entity.setTenantId(tenantId);
                entity.setStoreId(storeId);
                entity.setCapability(item.getCapability());
                entity.setEnabled(Boolean.TRUE.equals(item.getEnabled()));
                entity.setConfigJson(item.getConfigJson());
                entity.setIsDeleted(false);
                bcStoreCapabilityService.save(entity);
            }
        }

        bumpStoreConfigVersion(tenantId, storeId, expectedVersion);
    }

    /**
     * 更新常规营业时间（先删后插），并递增版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOpeningHours(UpdateStoreOpeningHoursCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");

        Long tenantId = command.getTenantId();
        Long storeId = command.getStoreId();
        Long expectedVersion = command.getExpectedConfigVersion();

        bcStoreOpeningHoursService.lambdaUpdate()
                .eq(BcStoreOpeningHours::getTenantId, tenantId)
                .eq(BcStoreOpeningHours::getStoreId, storeId)
                .set(BcStoreOpeningHours::getIsDeleted, true)
                .update();

        if (command.getSchedule() != null && command.getSchedule().getRegularHours() != null) {
            command.getSchedule().getRegularHours().forEach(item -> {
                BcStoreOpeningHours entity = new BcStoreOpeningHours();
                entity.setTenantId(tenantId);
                entity.setStoreId(storeId);
                entity.setWeekday((byte) item.getWeekday());
                entity.setStartTime(item.getStartTime());
                entity.setEndTime(item.getEndTime());
                entity.setPeriodType(item.getPeriodType());
                entity.setIsDeleted(false);
                bcStoreOpeningHoursService.save(entity);
            });
        }

        bumpStoreConfigVersion(tenantId, storeId, expectedVersion);
    }

    /**
     * 更新特殊日（先删后插），并递增版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSpecialDays(UpdateStoreSpecialDaysCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");

        Long tenantId = command.getTenantId();
        Long storeId = command.getStoreId();
        Long expectedVersion = command.getExpectedConfigVersion();

        bcStoreSpecialDayService.lambdaUpdate()
                .eq(BcStoreSpecialDay::getTenantId, tenantId)
                .eq(BcStoreSpecialDay::getStoreId, storeId)
                .set(BcStoreSpecialDay::getIsDeleted, true)
                .update();

        if (command.getSpecialDays() != null) {
            command.getSpecialDays().forEach(item -> {
                BcStoreSpecialDay entity = new BcStoreSpecialDay();
                entity.setTenantId(tenantId);
                entity.setStoreId(storeId);
                entity.setDate(item.getDate());
                entity.setSpecialType(item.getSpecialType());
                entity.setStartTime(item.getStartTime());
                entity.setEndTime(item.getEndTime());
                entity.setRemark(item.getRemark());
                entity.setIsDeleted(false);
                bcStoreSpecialDayService.save(entity);
            });
        }

        bumpStoreConfigVersion(tenantId, storeId, expectedVersion);
    }

    /**
     * 切换门店状态（OPEN/PAUSED/CLOSED），乐观锁控制。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(ChangeStoreStatusCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");
        Objects.requireNonNull(command.getStatus(), "status 不能为空");

        Long tenantId = command.getTenantId();
        Long storeId = command.getStoreId();
        Long expectedVersion = command.getExpectedConfigVersion();

        LambdaUpdateWrapper<BcStore> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .eq(BcStore::getConfigVersion, expectedVersion)
                .set(BcStore::getStatus, command.getStatus())
                .set(BcStore::getConfigVersion, expectedVersion + 1);

        boolean updated = bcStoreService.update(wrapper);
        if (!updated) {
            throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "切换门店状态失败，可能存在并发修改，请刷新后重试");
        }
        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, expectedVersion + 1);
    }

    /**
     * 切换接单开关，乐观锁控制。
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleOpenForOrders(ToggleOpenForOrdersCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");
        Objects.requireNonNull(command.getOpenForOrders(), "openForOrders 不能为空");

        Long tenantId = command.getTenantId();
        Long storeId = command.getStoreId();
        Long expectedVersion = command.getExpectedConfigVersion();

        LambdaUpdateWrapper<BcStore> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .eq(BcStore::getConfigVersion, expectedVersion)
                .set(BcStore::getOpenForOrders, command.getOpenForOrders())
                .set(BcStore::getConfigVersion, expectedVersion + 1);

        boolean updated = bcStoreService.update(wrapper);
        if (!updated) {
            throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "切换接单开关失败，可能存在并发修改，请刷新后重试");
        }
        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, expectedVersion + 1);
    }

    /**
     * 统一的版本递增封装，便于复用。
     */
    private void bumpStoreConfigVersion(Long tenantId, Long storeId, Long expectedVersion) {
        LambdaUpdateWrapper<BcStore> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .eq(BcStore::getConfigVersion, expectedVersion)
                .set(BcStore::getConfigVersion, expectedVersion + 1);
        boolean updated = bcStoreService.update(wrapper);
        if (!updated) {
            throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "更新门店配置版本失败，可能存在并发修改，请刷新后重试");
        }
        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, expectedVersion + 1);
    }
}
