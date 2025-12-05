package com.bluecone.app.product.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店菜单快照视图，用于 C 端高并发菜单读取。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuSnapshotView {

    private Long tenantId;
    private Long storeId;

    private String channel;
    private String orderScene;

    private Long version;
    private String menuJson;
    private String generatedAt;
}
