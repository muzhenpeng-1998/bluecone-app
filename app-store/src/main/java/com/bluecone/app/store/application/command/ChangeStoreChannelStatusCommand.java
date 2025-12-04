package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调整门店渠道状态（启用/停用）的命令。
 * <p>多租户：必须携带 tenantId + storeId；高稳定：预留 configVersion 供并发控制。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStoreChannelStatusCommand {

    private Long tenantId;
    private Long storeId;
    private Long channelId;
    private String targetStatus;
    private Long configVersion;
    private Long operatorId;
}
