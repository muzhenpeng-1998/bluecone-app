package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解绑门店渠道（逻辑删除）的命令。
 * <p>高隔离：仅作为应用层输入契约，后续由领域/基础设施完成删除。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnbindStoreChannelCommand {

    private Long tenantId;
    private Long storeId;
    private Long channelId;
    private Long operatorId;
}
