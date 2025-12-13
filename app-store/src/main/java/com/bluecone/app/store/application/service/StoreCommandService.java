package com.bluecone.app.store.application.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.store.application.command.ChangeStoreStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreCommand;
import com.bluecone.app.store.application.command.ToggleOpenForOrdersCommand;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.application.command.UpdateStoreCapabilitiesCommand;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.store.application.command.UpdateStoreSpecialDaysCommand;
import com.bluecone.app.core.idresolve.api.PublicIdRegistrar;
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
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.store.domain.model.StoreCapabilityModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 店铺写侧应用服务（Command Side）。
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>封装所有对门店主表及其能力、营业时间、特殊日配置等相关���写操作；</li>
 *     <li>基于 {@code configVersion} 字段，对配置相关写操作进行乐观锁控制，防止并发覆盖写；</li>
 *     <li>在写操作成功后，统一调用 {@link StoreConfigChangeService} 通知配置变更。</li>
 * </ul>
 *
 * <p>设计说明：</p>
 * <ul>
 *     <li>属于应用服务层，对上由 Facade/Controller 调用，对下协调 Entity/DAO；</li>
 *     <li>不暴露具体的 Mapper/Entity 给上层，保证依赖方向清晰、边界明确；</li>
 *     <li>统一抛出 {@link BizException}，并使用 {@link StoreErrorCode} 中定义的错误码。</li>
 * </ul>
 */
@Service
public class StoreCommandService {

    /**
     * 日志记录器，用于输出关键操作日志（如创建门店等）。
     */
    private static final Logger log = LoggerFactory.getLogger(StoreCommandService.class);

    /**
     * 门店主表服务，负责门店基础信息、状态、配置版本等数据的持久化。
     */
    private final IBcStoreService bcStoreService;
    /**
     * 门店能力配置表服务，负责门店支持的能力（堂食/外卖/自取/预约等）的持久化。
     */
    private final IBcStoreCapabilityService bcStoreCapabilityService;
    /**
     * 门店常规营业时间表服务。
     */
    private final IBcStoreOpeningHoursService bcStoreOpeningHoursService;
    /**
     * 门店特殊日（节假日/临时调整等）配置表服务。
     */
    private final IBcStoreSpecialDayService bcStoreSpecialDayService;
    /**
     * 门店配置变更通知服务，在配置修改成功后触发缓存失效或下游事件。
     */
    private final StoreConfigChangeService storeConfigChangeService;
    /**
     * ID 生成服务，用于生成门店相关 ULID。
     */
    private final IdService idService;
    /**
     * PublicId 编解码器，用于生成对外可见的门店编码。
     */
    private final PublicIdCodec publicIdCodec;
    /**
     * 公共 ID 映射注册器，在创建门店时写入 bc_public_id_map。
     */
    private final PublicIdRegistrar publicIdRegistrar;

    /**
     * 构造器注入门店相关持久化服务及配置变更服务。
     *
     * @param bcStoreService             门店主表服务
     * @param bcStoreCapabilityService   门店能力配置表服务
     * @param bcStoreOpeningHoursService 门店常规营业时间表服务
     * @param bcStoreSpecialDayService   门店特殊日配置表服务
     * @param storeConfigChangeService   门店配置变更通知服务
     */
    public StoreCommandService(IBcStoreService bcStoreService,
                               IBcStoreCapabilityService bcStoreCapabilityService,
                               IBcStoreOpeningHoursService bcStoreOpeningHoursService,
                               IBcStoreSpecialDayService bcStoreSpecialDayService,
                               StoreConfigChangeService storeConfigChangeService,
                               IdService idService,
                               PublicIdCodec publicIdCodec,
                               PublicIdRegistrar publicIdRegistrar) {
        // 保存门店主表服务引用，后续用于新增/更新门店基础数据
        this.bcStoreService = bcStoreService;
        // 保存门店能力配置表服务引用
        this.bcStoreCapabilityService = bcStoreCapabilityService;
        // 保存门店常规营业时间表服务引用
        this.bcStoreOpeningHoursService = bcStoreOpeningHoursService;
        // 保存门店特殊日配置表服务引用
        this.bcStoreSpecialDayService = bcStoreSpecialDayService;
        // 保存配置变更通知服务引用
        this.storeConfigChangeService = storeConfigChangeService;
        this.idService = idService;
        this.publicIdCodec = publicIdCodec;
        this.publicIdRegistrar = publicIdRegistrar;
    }

    /**
     * 创建门店及默认配置（当前默认配置仅指主表字段，能力/营业时间等通过后续接口维护）。
     *
     * <p>流程概述：</p>
     * <ol>
     *     <li>校验必填参数（租户、门店名称、行业类型）；</li>
     *     <li>生成 internal_id/public_id/store_no 并保证门店编码在租户内唯一；</li>
     *     <li>写入门店主表，初始化 {@code configVersion = 1}；</li>
     *     <li>触发配置变更通知，便于缓存或下游系统感知新门店。</li>
     * </ol>
     *
     * <p>该方法主要用于非幂等场景；在接入 {@code IdempotentCreateTemplate} 时，推荐调用
     * {@link #createStoreWithPreallocatedIds(CreateStoreCommand, Ulid128, String, Long)}，由模板统一生成 ID。</p>
     *
     * @param command 创建门店命令对象
     * @return 新建门店的对外 public_id
     */
    @Transactional(rollbackFor = Exception.class)
    public String createStore(CreateStoreCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getName(), "门店名称不能为空");
        Objects.requireNonNull(command.getIndustryType(), "行业类型不能为空");

        Ulid128 internalId = idService.nextUlid();
        String publicId = publicIdCodec.encode("sto", internalId).asString();
        Long storeNo;
        try {
            storeNo = idService.nextLongId();
        } catch (UnsupportedOperationException ex) {
            storeNo = null;
        }

        createStoreWithPreallocatedIds(command, internalId, publicId, storeNo);
        return publicId;
    }

    /**
     * 使用预生成的 internalId/publicId/storeNo 创建门店。
     *
     * <p>该方法不会再次生成 ID，适用于幂等创建模板等场景。</p>
     *
     * @param command    创建门店命令对象
     * @param internalId 预生成的内部 ULID
     * @param publicId   预生成的对外 public_id
     * @param storeNo    预生成的门店数字编号（可为 null 表示暂不使用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void createStoreWithPreallocatedIds(CreateStoreCommand command,
                                               Ulid128 internalId,
                                               String publicId,
                                               Long storeNo) {
        // 校验必填字段，避免后续持久化或业务逻辑出现空指针
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getName(), "门店名称不能为空");
        Objects.requireNonNull(command.getIndustryType(), "行业类型不能为空");

        // 当前操作所在租户
        Long tenantId = command.getTenantId();

        // 生成/校验门店编码：
        // 1）若未传入编码，则使用 public_id 作为 storeCode；
        // 2）若传入编码，则在当前租户下做唯一性校验。
        String storeCode = command.getStoreCode();
        if (storeCode == null || storeCode.isBlank()) {
            storeCode = publicId;
        } else {
            long cnt = bcStoreService.lambdaQuery()
                    .eq(BcStore::getTenantId, tenantId)
                    .eq(BcStore::getStoreCode, storeCode)
                    .eq(BcStore::getIsDeleted, false)
                    .count();
            // 若统计数量 > 0，则说明同租户下已有相同门店编码，属于配置冲突
            if (cnt > 0) {
                throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "门店编码已存在");
            }
        }

        // 构造门店主实体并填充基础属性
        BcStore entity = new BcStore();
        // 归属租户
        entity.setTenantId(tenantId);
        // 内部/对外 ID
        entity.setInternalId(internalId);
        entity.setPublicId(publicId);
        // 数字门店编号
        entity.setStoreNo(storeNo);
        // 门店编码（系统生成或前端传入）
        entity.setStoreCode(storeCode);
        // 门店名称
        entity.setName(command.getName());
        // 门店简称
        entity.setShortName(command.getShortName());
        // 行业类型
        entity.setIndustryType(command.getIndustryType());
        // 城市编码
        entity.setCityCode(command.getCityCode());
        // 新建门店默认状态设置为 OPEN
        entity.setStatus("OPEN");
        // 接单开关，默认 true，除非命令显式传入 false
        entity.setOpenForOrders(Boolean.TRUE.equals(command.getOpenForOrders()));
        // 初始化配置版本号为 1，后续所有配置更新都基于此版本做乐观锁
        entity.setConfigVersion(1L);
        entity.setCreatedAt(LocalDateTime.now());
        // 逻辑删除标记，默认未删除
        entity.setIsDeleted(false);
        // TODO 审计字段补充（createdAt/createdBy 等），可根据整体审计方案统一处理

        // 写入门店主表
        bcStoreService.save(entity);

        // 将 publicId 映射到内部 ULID，在同一事务内写入映射表。
        publicIdRegistrar.register(tenantId, com.bluecone.app.id.api.ResourceType.STORE, publicId, internalId);

        // 自增主键作为门店 ID
        Long storeId = entity.getId();
        // 默认能力/营业时间等配置当前暂不初始化，留给后续更新接口处理

        // 首次创建即认为配置有变更，触发一次配置变更通知（版本号为 1）
        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, entity.getConfigVersion());
        // 打印关键日志，方便排查问题或做审计（仅打印 public_id 前缀）
        String publicIdPrefix = publicId != null ? publicId.substring(0, Math.min(8, publicId.length())) : null;
        log.info("[createStore] tenantId={}, storeId={}, publicIdPrefix={}, storeCode={}, storeNo={}",
                tenantId, storeId, publicIdPrefix, storeCode, storeNo);
    }

    /**
     * 更新门店基础信息，使用 configVersion 乐观锁。
     *
     * <p>仅更新门店主表中的基础属性（名称、简称、行业、城市、接单开关等），不涉及能力、营业时间等配置表。</p>
     *
     * @param command 更新门店基础信息命令对象，包含期望配置版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStoreBase(UpdateStoreBaseCommand command) {
        // 校验必要参数：租户、门店 ID、期望配置版本号
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");

        // 当前操作所属租户
        Long tenantId = command.getTenantId();
        // 被修改的门店
        Long storeId = command.getStoreId();
        // 调用方认为当前门店配置版本
        Long expectedVersion = command.getExpectedConfigVersion();

        // 构造乐观锁更新条件：
        // 1）限定租户 + 门店；
        // 2）只更新未删除记录；
        // 3）configVersion 必须等于调用方传入的 expectedVersion。
        LambdaUpdateWrapper<BcStore> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .eq(BcStore::getConfigVersion, expectedVersion);

        // 以下为“按需更新”，只有字段不为 null 时才设置更新值，避免把未填写字段覆盖为 null
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

        // 基础信息更新成功后，配置版本号加 1
        wrapper.set(BcStore::getConfigVersion, expectedVersion + 1);

        // 执行更新，如果因版本不匹配或记录不存在导致更新失败，则抛出并发冲突异常
        boolean updated = bcStoreService.update(wrapper);
        if (!updated) {
            throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "更新门店基础信息失败，可能存在并发修改，请刷新后重试");
        }
        // 更新成功后触发配置变更通知，便于缓存或下游系统刷新最新配置
        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, expectedVersion + 1);
    }

    /**
     * 批量更新能力配置：先删后插，再递增版本。
     *
     * <p>实现方式：先将旧能力记录全部软删，再根据命令中的列表重建记录，最后递增配置版本。</p>
     *
     * @param command 更新能力配置命令对象，包含期望配置版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCapabilities(UpdateStoreCapabilitiesCommand command) {
        // 校验必要参数
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");

        // 当前操作所属租户
        Long tenantId = command.getTenantId();
        // 被修改的门店
        Long storeId = command.getStoreId();
        // 调用方认为当前门店配置版本
        Long expectedVersion = command.getExpectedConfigVersion();

        // 将该门店现有的所有能力记录全部软删（isDeleted = true）
        bcStoreCapabilityService.lambdaUpdate()
                .eq(BcStoreCapability::getTenantId, tenantId)
                .eq(BcStoreCapability::getStoreId, storeId)
                .set(BcStoreCapability::getIsDeleted, true)
                .update();

        // 若命令中携带新的能力配置列表，则逐条插入
        if (command.getCapabilities() != null) {
            for (StoreCapabilityModel item : command.getCapabilities()) {
                // 构造能力配置持久化实体
                BcStoreCapability entity = new BcStoreCapability();
                entity.setTenantId(tenantId);
                entity.setStoreId(storeId);
                // 能力标识（例如 DINE_IN、TAKE_OUT 等）
                entity.setCapability(item.getCapability());
                // 能力是否开���
                entity.setEnabled(Boolean.TRUE.equals(item.getEnabled()));
                // 能力扩展配置 JSON
                entity.setConfigJson(item.getConfigJson());
                // 新插入记录标记为未删除
                entity.setIsDeleted(false);
                // 保存能力配置记录
                bcStoreCapabilityService.save(entity);
            }
        }

        // 主表配置版本号 +1，并触发配置变更通知
        bumpStoreConfigVersion(tenantId, storeId, expectedVersion);
    }

    /**
     * 更新常规营业时间（先删后插），并递增版本。
     *
     * <p>实现方式：先软删该门店现有的常规营业时间记录，再重建命令中提供的 regularHours。</p>
     *
     * @param command 更新常规营业时间命令对象，包含日程及期望配置版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOpeningHours(UpdateStoreOpeningHoursCommand command) {
        // 校验必要参数
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");

        // 当前操作所属租户
        Long tenantId = command.getTenantId();
        // 被修改的门店
        Long storeId = command.getStoreId();
        // 调用方认为当前门店配置版本
        Long expectedVersion = command.getExpectedConfigVersion();

        // 将该门店现有的常规营业时间记录全部软删
        bcStoreOpeningHoursService.lambdaUpdate()
                .eq(BcStoreOpeningHours::getTenantId, tenantId)
                .eq(BcStoreOpeningHours::getStoreId, storeId)
                .set(BcStoreOpeningHours::getIsDeleted, true)
                .update();

        // 若命令中携带新的常规营业时间配置，则逐条插入
        if (command.getSchedule() != null && command.getSchedule().getRegularHours() != null) {
            command.getSchedule().getRegularHours().forEach(item -> {
                // 构造常规营业时间持久化实体
                BcStoreOpeningHours entity = new BcStoreOpeningHours();
                entity.setTenantId(tenantId);
                entity.setStoreId(storeId);
                // 星期几（1-7），这里转成 byte 存储
                entity.setWeekday((byte) item.getWeekday());
                // 当日开始营业时间
                entity.setStartTime(item.getStartTime());
                // 当日结束营业时间
                entity.setEndTime(item.getEndTime());
                // 时间段类型（如全天、多时段等），具体语义由领域约定
                entity.setPeriodType(item.getPeriodType());
                // 新插入记录标记为未删除
                entity.setIsDeleted(false);
                // 保存该条营业时间记录
                bcStoreOpeningHoursService.save(entity);
            });
        }

        // 主表配置版本号 +1，并触发配置变更通知
        bumpStoreConfigVersion(tenantId, storeId, expectedVersion);
    }

    /**
     * 更新特殊日（先删后插），并递增版本。
     *
     * <p>实现方式：先软删该门店现有的特殊日记录，再重建命令中提供的 specialDays。</p>
     *
     * @param command 更新特殊日配置命令对象，包含特殊日列表及期望配置版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSpecialDays(UpdateStoreSpecialDaysCommand command) {
        // 校验必要参数
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");

        // 当前操作所属租户
        Long tenantId = command.getTenantId();
        // 被修改的门店
        Long storeId = command.getStoreId();
        // 调用方认为当前门店配置版本
        Long expectedVersion = command.getExpectedConfigVersion();

        // 将该门店现有的特殊日配置记录全部软删
        bcStoreSpecialDayService.lambdaUpdate()
                .eq(BcStoreSpecialDay::getTenantId, tenantId)
                .eq(BcStoreSpecialDay::getStoreId, storeId)
                .set(BcStoreSpecialDay::getIsDeleted, true)
                .update();

        // 若命令中携带新的特殊日配置，则逐条插入
        if (command.getSpecialDays() != null) {
            command.getSpecialDays().forEach(item -> {
                // 构造特殊日配置持久化实体
                BcStoreSpecialDay entity = new BcStoreSpecialDay();
                entity.setTenantId(tenantId);
                entity.setStoreId(storeId);
                // 特殊日期
                entity.setDate(item.getDate());
                // 特殊类型（如节假日延长营业、临时停业等）
                entity.setSpecialType(item.getSpecialType());
                // 特殊日开始时间
                entity.setStartTime(item.getStartTime());
                // 特殊日结束时间
                entity.setEndTime(item.getEndTime());
                // 备注信息，方便运营同学了解配置原因
                entity.setRemark(item.getRemark());
                // 新插入记录标记为未删除
                entity.setIsDeleted(false);
                // 保存该条特殊日记录
                bcStoreSpecialDayService.save(entity);
            });
        }

        // 主表配置版本号 +1，并触发配置变更通知
        bumpStoreConfigVersion(tenantId, storeId, expectedVersion);
    }

    /**
     * 切换门店状态（OPEN/PAUSED/CLOSED），乐观锁控制。
     *
     * <p>仅更新门店主表中的 {@code status} 字段，并使 {@code configVersion} + 1。</p>
     *
     * @param command 切换门店状态命令对象，包含目标状态及期望配置版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(ChangeStoreStatusCommand command) {
        // 校验必要参数
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");
        Objects.requireNonNull(command.getStatus(), "status 不能为空");

        // 当前操作所属租户
        Long tenantId = command.getTenantId();
        // 被修改的门店
        Long storeId = command.getStoreId();
        // 调用方认为当前门店配置版本
        Long expectedVersion = command.getExpectedConfigVersion();

        // 构造乐观锁更新条件：
        // 1）限定租户 + 门店；
        // 2）只更新未删除记录；
        // 3）configVersion 必须等于 expectedVersion。
        LambdaUpdateWrapper<BcStore> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .eq(BcStore::getConfigVersion, expectedVersion)
                // 更新门店状态为目标状态
                .set(BcStore::getStatus, command.getStatus())
                // 成功更新后配置版本号 +1
                .set(BcStore::getConfigVersion, expectedVersion + 1);

        // 执行更新操作，若因版本不匹配或记录不存在导致更新失败，则视为并发冲突
        boolean updated = bcStoreService.update(wrapper);
        if (!updated) {
            throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "切换门店状态失败，可能存在并发修改，请刷新后重试");
        }
        // 更新成功后触发配置变更通知
        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, expectedVersion + 1);
    }

    /**
     * 切换接单开关，乐观锁控制。
     *
     * <p>仅更新门店主表中的 {@code openForOrders} 字段，并使 {@code configVersion} + 1。</p>
     *
     * @param command 切换接单开关命令对象，包含目标开关值及期望配置版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleOpenForOrders(ToggleOpenForOrdersCommand command) {
        // 校验必要参数
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getExpectedConfigVersion(), "expectedConfigVersion 不能为空");
        Objects.requireNonNull(command.getOpenForOrders(), "openForOrders 不能为空");

        // 当前操作所属租户
        Long tenantId = command.getTenantId();
        // 被修改的门店
        Long storeId = command.getStoreId();
        // 调用方认为当前门店配置版本
        Long expectedVersion = command.getExpectedConfigVersion();

        // 构造乐观锁更新条件，与 changeStatus 类似
        LambdaUpdateWrapper<BcStore> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .eq(BcStore::getConfigVersion, expectedVersion)
                // 更新接单开关状态
                .set(BcStore::getOpenForOrders, command.getOpenForOrders())
                // 成功更新后配置版本号 +1
                .set(BcStore::getConfigVersion, expectedVersion + 1);

        // 执行更新操作，若因版本不匹配或记录不存在导致更新失败，则视为并发冲突
        boolean updated = bcStoreService.update(wrapper);
        if (!updated) {
            throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "切换接单开关失败，可能存在并发修改，请刷新后重试");
        }
        // 更新成功后触发配置变更通知
        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, expectedVersion + 1);
    }

    /**
     * 统一的版本递增封装，便于复用。
     *
     * <p>仅更新门店主表中的 {@code configVersion} 字段，将其从期望版本递增为 {@code expectedVersion + 1}。</p>
     *
     * @param tenantId        租户 ID
     * @param storeId         门店 ID
     * @param expectedVersion 调用方持有的当前配置版本号
     */
    private void bumpStoreConfigVersion(Long tenantId, Long storeId, Long expectedVersion) {
        // 构造仅更新 configVersion 的乐观锁更新条件
        LambdaUpdateWrapper<BcStore> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .eq(BcStore::getConfigVersion, expectedVersion)
                // 将配置版本号更新为 expectedVersion + 1
                .set(BcStore::getConfigVersion, expectedVersion + 1);
        // 执行更新操作，若因版本不匹配或记录不存在导致更新失败，则视为并发冲突
        boolean updated = bcStoreService.update(wrapper);
        if (!updated) {
            throw new BizException(StoreErrorCode.STORE_CONFIG_CONFLICT, "更新门店配置版本失败，可能存在并发修改，请刷新后重试");
        }
        // 更新成功后触发配置变更通知
        storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, expectedVersion + 1);
    }
}
