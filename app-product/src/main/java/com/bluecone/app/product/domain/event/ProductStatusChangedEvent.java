package com.bluecone.app.product.domain.event;

import com.bluecone.app.product.domain.enums.ProductStatus;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品状态变更的领域事件，记录状态变更前后以便下游做同步或补偿。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatusChangedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private Long productId;

    private ProductStatus beforeStatus;

    private ProductStatus afterStatus;
}
