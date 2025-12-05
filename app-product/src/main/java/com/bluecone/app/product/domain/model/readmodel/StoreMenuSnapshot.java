package com.bluecone.app.product.domain.model.readmodel;

import com.bluecone.app.product.domain.enums.MenuScene;
import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.enums.SaleChannel;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店菜单快照读模型，对应 bc_store_menu_snapshot，支撑高并发的菜单拉取场景。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long storeId;

    private SaleChannel channel;

    private MenuScene scene;

    private Long version;

    private String menuJson;

    private LocalDateTime generatedAt;

    private ProductStatus status;
}
