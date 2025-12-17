package com.bluecone.app.store.application.service;

import com.bluecone.app.store.api.dto.StoreChannelView;
import com.bluecone.app.store.dao.entity.BcStoreChannel;
import org.springframework.stereotype.Component;

/**
 * 渠道装配器：实体 → 对外视图 DTO。
 * <p>高隔离：仅在应用层内部使用，隐藏底层实体结构。</p>
 */
@Component
public class StoreChannelAssembler {

    /**
     * 实体转换为对外视图 DTO。
     */
    public StoreChannelView toView(BcStoreChannel entity) {
        if (entity == null) {
            return null;
        }
        StoreChannelView view = new StoreChannelView();
        view.setTenantId(entity.getTenantId());
        view.setStoreId(entity.getStoreId());
        view.setChannelId(entity.getId());
        view.setChannelType(entity.getChannelType());
        view.setExternalStoreId(entity.getExternalStoreId());
        view.setAppId(entity.getAppId());
        view.setStatus(entity.getStatus());
        view.setConfigSummary(parseConfigSummary(entity.getConfigJson())); // 解析配置摘要：提取 JSON 关键信息或前 50 字符
        view.setCreatedAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
        view.setUpdatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
        return view;
    }

    /**
     * 解析渠道配置 JSON 摘要。
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
        // 后续可扩展为解析 JSON 并提取关键字段（如授权信息、回调地址等）
        return configJson.length() > 50 ? configJson.substring(0, 50) + "..." : configJson;
    }
}
