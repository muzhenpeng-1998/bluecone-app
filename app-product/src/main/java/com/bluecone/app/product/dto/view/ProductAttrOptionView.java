package com.bluecone.app.product.dto.view;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 属性项视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttrOptionView {

    private Long optionId;
    private String name;
    private String valueCode;
    private BigDecimal priceDelta;
    private Integer sortOrder;
    private Integer status;
}
