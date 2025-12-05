package com.bluecone.app.product.domain.model.menu;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单规格项视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuSpecOptionView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long optionId;
    private String name;
    private BigDecimal priceDelta;
    private Boolean defaultOption;
    private Integer sortOrder;
}
