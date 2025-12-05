package com.bluecone.app.product.domain.model.menu;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单小料项视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuAddonItemView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long addonItemId;
    private String name;
    private BigDecimal price;
    private BigDecimal maxQuantity;
    private BigDecimal freeLimit;
    private Integer sortOrder;
}
