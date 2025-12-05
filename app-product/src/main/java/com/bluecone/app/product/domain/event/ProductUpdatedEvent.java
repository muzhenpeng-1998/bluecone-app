package com.bluecone.app.product.domain.event;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品信息变更的领域事件，占位用于同步缓存、索引等下游。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private Long productId;
}
