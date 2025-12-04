package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新门店渠道配置的命令。
 * <p>高稳定：预留 configVersion 供乐观锁或版本控制使用。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreChannelConfigCommand {

    private Long tenantId;
    private Long storeId;
    private Long channelId;
    private String configJson;
    private Long configVersion;
    private Long operatorId;
}
