package com.bluecone.app.product.domain.model.store;

import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.enums.SaleChannel;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店维度的商品配置领域模型，对应 bc_product_store_config，控制门店/渠道可见性与定价策略。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long productId;

    private Long skuId;

    private SaleChannel channel;

    private boolean visible;

    private BigDecimal overridePrice;

    private List<String> availableOrderTypes;

    private List<TimeRange> availableTimeRanges;

    private Integer dailySoldOutLimit;

    private Integer sortOrder;

    private ProductStatus status;
}
