package com.bluecone.app.product.domain.event;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品创建完成的领域事件，占位用于后续通知搜索、库存等下游。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private Long productId;
}
