package com.bluecone.app.store.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店渠道绑定视图（只读 DTO）。
 * <p>高隔离：对外仅暴露绑定信息，不包含内部实体/Mapper 细节。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreChannelView {

    private Long tenantId;
    private Long storeId;
    private Long channelId;
    private String channelType;
    private String externalStoreId;
    private String appId;
    private String status;
    private String configSummary;
    private Long createdAt;
    private Long updatedAt;
}
