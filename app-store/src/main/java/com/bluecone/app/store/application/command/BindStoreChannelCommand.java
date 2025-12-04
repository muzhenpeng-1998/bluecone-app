package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 绑定门店外部渠道的命令。
 * <p>高隔离：作为应用层入参模型，承载租户/门店/渠道信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindStoreChannelCommand {

    private Long tenantId;
    private Long storeId;
    private String channelType;
    private String externalStoreId;
    private String appId;
    private String configJson;
    private Long operatorId;
}
