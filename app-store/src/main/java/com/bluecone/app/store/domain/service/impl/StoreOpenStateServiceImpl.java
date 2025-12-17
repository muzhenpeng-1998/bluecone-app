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

    @Override
    public StoreOrderAcceptResult check(StoreConfig config, String capability, LocalDateTime now, String channelType) {
        // 1）配置判空
        if (config == null) {
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_NOT_FOUND.getCode())
                    .reasonMessage(StoreErrorCode.STORE_NOT_FOUND.getMessage())
                    .build();
        }

        // 2）检查门店状态
        if (!"OPEN".equalsIgnoreCase(config.getStatus())) {
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_STATUS_NOT_OPEN.getCode())
                    .reasonMessage(StoreErrorCode.STORE_STATUS_NOT_OPEN.getMessage())
                    .build();
        }

        // 3）接单开关
        if (!config.isOpenForOrders()) {
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_NOT_ACCEPTING_ORDERS.getCode())
                    .reasonMessage(StoreErrorCode.STORE_NOT_ACCEPTING_ORDERS.getMessage())
                    .build();
        }

        // 4）能力校验
        if (capability != null && !capability.isBlank()) {
            boolean enabled = config.getCapabilities() != null && config.getCapabilities().stream()
                    .filter(item -> item.getCapability() != null)
                    .anyMatch(item -> capability.equalsIgnoreCase(item.getCapability()) && Boolean.TRUE.equals(item.getEnabled()));
            if (!enabled) {
                return StoreOrderAcceptResult.builder()
                        .acceptable(false)
                        .reasonCode(StoreErrorCode.STORE_CAPABILITY_DISABLED.getCode())
                        .reasonMessage(StoreErrorCode.STORE_CAPABILITY_DISABLED.getMessage())
                        .build();
            }
        }

        // 5）营业时间 / 特殊日判定
        // 说明：优先检查特殊日（如节假日停业），再检查常规营业时间，确保特殊日配置优先级最高
        if (config.getOpeningSchedule() == null) {
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_NO_OPENING_CONFIG.getCode())
                    .reasonMessage(StoreErrorCode.STORE_NO_OPENING_CONFIG.getMessage())
                    .build();
        }
        // 调用 StoreOpeningSchedule.isOpenAt() 方法，内部已实现特殊日优先、常规营业时间次之的判断逻辑
        boolean open = config.getOpeningSchedule().isOpenAt(now);
        if (!open) {
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode(StoreErrorCode.STORE_OUT_OF_BUSINESS_HOURS.getCode())
                    .reasonMessage(StoreErrorCode.STORE_OUT_OF_BUSINESS_HOURS.getMessage())
                    .build();
        }

        // 6）渠道校验
        // 说明：检查订单来源渠道是否已绑定且状态为启用，避免从未绑定渠道下单
        if (channelType != null && !channelType.isBlank()) {
            boolean channelValid = config.getChannels() != null && config.getChannels().stream()
                    .filter(channel -> channel != null && channel.getChannelType() != null)
                    .anyMatch(channel -> channelType.equalsIgnoreCase(channel.getChannelType())
                            && "ACTIVE".equalsIgnoreCase(channel.getStatus()));
            if (!channelValid) {
                log.debug("门店 {} 未绑定渠道 {} 或渠道状态非 ACTIVE", config.getStoreId(), channelType);
                return StoreOrderAcceptResult.builder()
                        .acceptable(false)
                        .reasonCode(StoreErrorCode.STORE_CHANNEL_NOT_BOUND.getCode())
                        .reasonMessage(StoreErrorCode.STORE_CHANNEL_NOT_BOUND.getMessage())
                        .build();
            }
        }

        // 7）全部通过，允许接单
        return StoreOrderAcceptResult.builder()
                .acceptable(true)
                .reasonCode("OK")
                .reasonMessage("允许接单")
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
