package com.bluecone.app.store.application.service;

import com.bluecone.app.store.api.dto.StoreDeviceView;
import com.bluecone.app.store.dao.entity.BcStoreDevice;
import org.springframework.stereotype.Component;

/**
 * 设备装配器：实体 → 对外视图 DTO。
 * <p>高隔离：屏蔽底层实体字段变化。</p>
 */
@Component
public class StoreDeviceAssembler {

    public StoreDeviceView toView(BcStoreDevice entity) {
        if (entity == null) {
            return null;
        }
        return StoreDeviceView.builder()
                .tenantId(entity.getTenantId())
                .storeId(entity.getStoreId())
                .deviceId(entity.getId())
                .deviceType(entity.getDeviceType())
                .name(entity.getName())
                .sn(entity.getSn())
                .status(entity.getStatus())
                .configJson(entity.getConfigJson())
                .configSummary(parseConfigSummary(entity.getConfigJson())) // 解析配置摘要：提取 JSON 关键信息或前 50 字符
                .build();
    }

    /**
     * 解析配置 JSON 摘要。
     * <p>最小实现：返回 JSON 前 50 字符，便于前端快速展示配置概览。</p>
     *
     * @param configJson 配置 JSON 字符串
     * @return 配置摘要（如为 null 或空则返回 null）
     */
    private String parseConfigSummary(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        // 最小实现：返回 JSON 前 50 字符作为摘要
        // 后续可扩展为解析 JSON 并提取关键字段（如设备型号、IP 地址等）
        return configJson.length() > 50 ? configJson.substring(0, 50) + "..." : configJson;
    }
}
