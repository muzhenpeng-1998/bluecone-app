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
        view.setConfigSummary(null); // TODO: 后续可从 configJson 解析摘要
        view.setCreatedAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
        view.setUpdatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
        return view;
    }
}
