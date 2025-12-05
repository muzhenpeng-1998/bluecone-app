package com.bluecone.app.product.dto.view;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小料项视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAddonItemView {

    private Long addonItemId;
    private String name;
    private BigDecimal price;
    private BigDecimal maxQuantity;
    private BigDecimal freeLimit;
    private Integer sortOrder;
    private Integer status;
}
