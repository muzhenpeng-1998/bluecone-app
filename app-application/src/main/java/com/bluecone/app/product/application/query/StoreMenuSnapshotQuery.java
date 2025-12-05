package com.bluecone.app.product.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店菜单快照查询对象，小程序/前端读取菜单的入参。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuSnapshotQuery {

    private Long storeId;
    private String channel;
    private String orderScene;
}
