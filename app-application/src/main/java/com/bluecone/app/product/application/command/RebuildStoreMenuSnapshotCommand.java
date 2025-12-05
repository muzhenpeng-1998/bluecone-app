package com.bluecone.app.product.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端触发门店菜单快照重建的命令对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebuildStoreMenuSnapshotCommand {

    private Long storeId;
    private String channel;
    private String orderScene;
    private Boolean fullRebuild;
}
