package com.bluecone.app.product.domain.model.menu;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单快照 View 模型，用于组装门店/渠道/场景下的菜单结构，便于序列化为 JSON。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuSnapshotModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long storeId;
    private String channel;
    private String orderScene;
    private Long version;

    private List<StoreMenuCategoryView> categories;
}
