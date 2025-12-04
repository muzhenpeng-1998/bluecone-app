package com.bluecone.app.store.domain.service.assembler;

import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.store.domain.model.StoreCapabilityModel;
import com.bluecone.app.store.domain.model.StoreChannelModel;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.dao.entity.BcStore;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 领域层装配器：负责将 StoreConfig 聚合裁剪为对外 DTO。
 * <p>高隔离：仅输出视图 DTO，避免外部模块直接依赖领域对象。</p>
 * <p>高并发：装配产物可直接作为快照缓存的载体，减少重复组装。</p>
 */
@Component
public class StoreViewAssembler {

    /**
     * 将主表实体转换为基础视图，供列表/详情直接使用。
     */
    public StoreBaseView toStoreBaseView(BcStore entity) {
        if (entity == null) {
            return null;
        }
        return StoreBaseView.builder()
                .tenantId(entity.getTenantId())
                .storeId(entity.getId())
                .storeCode(entity.getStoreCode())
                .name(entity.getName())
                .shortName(entity.getShortName())
                .industryType(entity.getIndustryType())
                .cityCode(entity.getCityCode())
                .status(entity.getStatus())
                .openForOrders(Boolean.TRUE.equals(entity.getOpenForOrders()))
                .logoUrl(entity.getLogoUrl())
                .coverUrl(entity.getCoverUrl())
                .build();
    }

    /**
     * 裁剪基础信息视图。
     */
    public StoreBaseView toStoreBaseView(StoreConfig config) {
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
                .openForOrders(config.isOpenForOrders())
                .build();
    }

    /**
     * 裁剪订单场景快照，仅保留高频字段。
     */
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

        return StoreOrderSnapshot.builder()
                .storeId(config.getStoreId())
                .storeName(config.getName())
                .cityCode(config.getCityCode())
                .industryType(config.getIndustryType())
                .status(config.getStatus())
                .openForOrders(config.isOpenForOrders())
                .enabledCapabilities(enabledCapabilities)
                .channelType(channelType)
                .channelStatus(matchedChannel != null ? matchedChannel.getStatus() : null)
                .configVersion(config.getConfigVersion())
                .build();
    }
}
