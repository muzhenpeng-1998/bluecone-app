package com.bluecone.app.store.api;

import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import java.time.LocalDateTime;

/**
 * 门店上下文提供器，对外暴露门店基础信息与下单快照的统一入口。
 */
public interface StoreContextProvider {

    /**
     * 获取门店基础信息视图（名称、状态等）。
     */
    StoreBaseView getStoreBase(Long tenantId, Long storeId);

    /**
     * 获取门店下单快照，包含是否可接单等信息。
     *
     * @param now         当前时间（可为空，默认取当前时间）
     * @param channelType 渠道类型，预留后续扩展
     */
    StoreOrderSnapshot getOrderSnapshot(Long tenantId, Long storeId, LocalDateTime now, String channelType);
}

