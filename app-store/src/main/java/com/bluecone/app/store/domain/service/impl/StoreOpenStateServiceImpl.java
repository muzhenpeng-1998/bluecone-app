package com.bluecone.app.store.domain.service.impl;

import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.model.runtime.StoreRuntime;
import com.bluecone.app.store.domain.service.StoreOpenStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 门店营业状态校验实现，集中封装接单规则。
 * <p>高稳定：为后续降级预留扩展点，例如 DB 异常时仍可基于缓存快照判断。</p>
 * <p>高并发：直接依赖 StoreConfig 快照，无需重复查询子表。</p>
 */
@Service
public class StoreOpenStateServiceImpl implements StoreOpenStateService {

    private static final Logger log = LoggerFactory.getLogger(StoreOpenStateServiceImpl.class);

    /**
     * 校验是否可接单，判断顺序固定并写中文注释。
     * <p>判断顺序说明：按照业务优先级，先检查基础配置（门店存在、状态、开关），再检查业务规则（渠道、营业时间），最后检查能力。</p>
     *
     * @param config      门店配置聚合（已确保门店存在且归属正确的 tenant）
     * @param capability  请求能力（可选，用于校验特定服务类型是否启用）
     * @param now         当前时间
     * @param channelType 渠道类型（可选，用于校验渠道绑定状态）
     * @return 是否可接单及原因（不允许返回 null）
     */
    @Override
    public StoreOrderAcceptResult check(StoreConfig config, String capability, LocalDateTime now, String channelType) {
        // 1）门店是否存在且归属 tenant
        // 说明：此判断优先级最高，门店不存在时直接返回，避免后续判断浪费资源
        // 注意：StoreConfig 在 loadStoreConfig 时已经校验了 tenant 归属，此处仅做判空
        if (config == null) {
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_NOT_FOUND.getCode())
                    .reasonMessage(StoreErrorCode.STORE_NOT_FOUND.getMessage())
                    .detail("门店不存在或已删除")
                    .build();
        }

        // 2）门店状态是否允许接单
        // 说明：门店状态必须在 OPEN 状态才能接单，CLOSED/PAUSED 等状态不允许接单
        // 顺序说明：状态检查在接单开关之前，因为状态是门店的基础属性，状态不对时不需要检查开关
        if (!"OPEN".equalsIgnoreCase(config.getStatus())) {
            String detail = String.format("门店状态为 %s，仅 OPEN 状态允许接单", config.getStatus());
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_STATUS_NOT_OPEN.getCode())
                    .reasonMessage(StoreErrorCode.STORE_STATUS_NOT_OPEN.getMessage())
                    .detail(detail)
                    .build();
        }

        // 3）openForOrders 开关
        // 说明：即使门店状态为 OPEN，如果接单开关关闭，也不允许接单
        // 顺序说明：开关检查在营业时间之前，因为开关是运营人员主动控制，优先级高于时间规则
        if (!config.isOpenForOrders()) {
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_NOT_ACCEPTING_ORDERS.getCode())
                    .reasonMessage(StoreErrorCode.STORE_NOT_ACCEPTING_ORDERS.getMessage())
                    .detail("门店接单开关已关闭")
                    .build();
        }

        // 4）渠道能力（若参数带 channelType）
        // 说明：如果提供了 channelType 参数，需要检查该渠道是否已绑定且状态为 ACTIVE
        // 顺序说明：渠道检查在营业时间之前，因为渠道绑定是前置条件，未绑定的渠道不应该下单
        if (channelType != null && !channelType.isBlank()) {
            boolean channelValid = config.getChannels() != null && config.getChannels().stream()
                    .filter(channel -> channel != null && channel.getChannelType() != null)
                    .anyMatch(channel -> channelType.equalsIgnoreCase(channel.getChannelType())
                            && "ACTIVE".equalsIgnoreCase(channel.getStatus()));
            if (!channelValid) {
                log.debug("门店 {} 未绑定渠道 {} 或渠道状态非 ACTIVE", config.getStoreId(), channelType);
                String detail = String.format("门店未绑定渠道 %s 或渠道状态非 ACTIVE", channelType);
                return StoreOrderAcceptResult.builder()
                        .acceptable(false)
                        .reasonCode(StoreErrorCode.STORE_CHANNEL_NOT_BOUND.getCode())
                        .reasonMessage(StoreErrorCode.STORE_CHANNEL_NOT_BOUND.getMessage())
                        .detail(detail)
                        .build();
            }
        }

        // 5）营业时间/特殊日
        // 说明：优先检查特殊日（如节假日停业），再检查常规营业时间，确保特殊日配置优先级最高
        // 顺序说明：营业时间检查放在最后，因为这是最细粒度的规则，需要在前置条件都满足后再检查
        if (config.getOpeningSchedule() == null) {
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_NO_OPENING_CONFIG.getCode())
                    .reasonMessage(StoreErrorCode.STORE_NO_OPENING_CONFIG.getMessage())
                    .detail("门店未配置营业时间")
                    .build();
        }
        // 调用 StoreOpeningSchedule.isOpenAt() 方法，内部已实现特殊日优先、常规营业时间次之的判断逻辑
        boolean open = config.getOpeningSchedule().isOpenAt(now);
        if (!open) {
            // 获取当天的营业时间区间，用于 detail 提示
            String todayRange = config.getOpeningSchedule().getOpeningHoursRange(now.toLocalDate());
            String detail = todayRange != null 
                    ? String.format("当前时间不在营业时间内，当天营业时间：%s", todayRange)
                    : "当前不在营业时间内，当天不营业";
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_OUT_OF_BUSINESS_HOURS.getCode())
                    .reasonMessage(StoreErrorCode.STORE_OUT_OF_BUSINESS_HOURS.getMessage())
                    .detail(detail)
                    .build();
        }

        // 6）能力校验（如果提供了 capability 参数）
        // 说明：检查特定服务类型（如外卖、堂食、自提）是否启用
        // 顺序说明：能力检查放在最后，因为这是可选校验，只有提供了 capability 参数时才检查
        if (capability != null && !capability.isBlank()) {
            boolean enabled = config.getCapabilities() != null && config.getCapabilities().stream()
                    .filter(item -> item.getCapability() != null)
                    .anyMatch(item -> capability.equalsIgnoreCase(item.getCapability()) && Boolean.TRUE.equals(item.getEnabled()));
            if (!enabled) {
                String detail = String.format("门店未启用能力：%s", capability);
                return StoreOrderAcceptResult.builder()
                        .acceptable(false)
                        .reasonCode(StoreErrorCode.STORE_CAPABILITY_DISABLED.getCode())
                        .reasonMessage(StoreErrorCode.STORE_CAPABILITY_DISABLED.getMessage())
                        .detail(detail)
                        .build();
            }
        }

        // 7）全部通过，允许接单
        return StoreOrderAcceptResult.builder()
                .acceptable(true)
                .reasonCode("OK")
                .reasonMessage("允许接单")
                .detail("所有校验通过")
                .build();
    }

    @Override
    public boolean isStoreOpenForOrder(StoreRuntime runtime, LocalDateTime now) {
        if (runtime == null) {
            return false;
        }
        // 1）后台强制打烊
        if (Boolean.TRUE.equals(runtime.getForceClosed())) {
            return false;
        }
        // 2）业务状态判定，示例：1=营业中，其他视为不可接单
        Integer bizStatus = runtime.getBizStatus();
        if (bizStatus == null) {
            return false;
        }
        if (bizStatus != 1) {
            return false;
        }
        // 3）暂不校验营业时段/节假日，后续扩展
        return true;
    }
}
