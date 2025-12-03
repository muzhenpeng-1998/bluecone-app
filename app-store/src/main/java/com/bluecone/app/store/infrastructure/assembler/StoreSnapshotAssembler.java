package com.bluecone.app.store.infrastructure.assembler;

import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.store.domain.model.StoreCapabilityModel;
import com.bluecone.app.store.domain.model.StoreChannelModel;
import com.bluecone.app.store.domain.model.StoreConfig;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 将领域聚合 StoreConfig 装配为对外只读视图（基础信息/订单快照）。
 * <p>高隔离：外部模块拿到的是 DTO，不会直接依赖领域模型。</p>
 * <p>高并发：快照装配后可直接缓存，减少重复装配成本。</p>
 */
@Component
public class StoreSnapshotAssembler {

    public StoreBaseView toBaseView(StoreConfig config) {
        if (config == null) {
            return null;
        }
        return StoreBaseView.builder()
                .tenantId(config.getTenantId())
                .storeId(config.getStoreId())
                .storeCode(config.getStoreCode())
                .name(config.getName())
                .shortName(config.getShortName())
                .industryType(config.getIndustryType())
                .cityCode(config.getCityCode())
                .status(config.getStatus())
                .openForOrders(config.getOpenForOrders())
                .build();
    }

    public StoreOrderSnapshot toOrderSnapshot(StoreConfig config, LocalDateTime now, String channelType) {
        if (config == null) {
            return null;
        }
        Set<String> enabledCapabilities = Optional.ofNullable(config.getCapabilities())
                .orElse(Collections.emptyList())
                .stream()
                .filter(cap -> Boolean.TRUE.equals(cap.getEnabled()))
                .map(StoreCapabilityModel::getCapability)
                .collect(Collectors.toSet());

        StoreChannelModel matchedChannel = Optional.ofNullable(config.getChannels())
                .orElse(Collections.emptyList())
                .stream()
                .filter(channel -> Objects.equals(channelType, channel.getChannelType()))
                .findFirst()
                .orElse(null);

        // TODO: 可在此处结合 StoreOpenStateService 计算 currentlyOpen/specialDayHit，并写入快照
        return StoreOrderSnapshot.builder()
                .storeId(config.getStoreId())
                .storeName(config.getName())
                .cityCode(config.getCityCode())
                .industryType(config.getIndustryType())
                .status(config.getStatus())
                .openForOrders(config.getOpenForOrders())
                .enabledCapabilities(enabledCapabilities)
                .channelType(channelType)
                .channelStatus(matchedChannel != null ? matchedChannel.getStatus() : null)
                .configVersion(config.getConfigVersion())
                .build();
    }
}
