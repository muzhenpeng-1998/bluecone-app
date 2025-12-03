package com.bluecone.app.store.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对应 bc_store_channel 的领域模型，描述门店在各渠道的绑定状态。
 * <p>用于校验订单来源渠道是否合法，避免上层直接依赖数据库表结构。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreChannelModel {

    private String channelType;

    private String externalStoreId;

    private String appId;

    private String configJson;

    private String status;
}
