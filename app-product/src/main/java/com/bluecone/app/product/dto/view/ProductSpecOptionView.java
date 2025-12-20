package com.bluecone.app.product.dto.view;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规格项视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecOptionView {

    private Long optionId;
    private String name;
    private BigDecimal priceDelta;
    private Boolean isDefault;
    private Integer sortOrder;
    private Integer status;
}
