package com.bluecone.app.product.domain.model.menu;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单属性项视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuAttrOptionView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long optionId;
    private String name;
    private String valueCode;
    private BigDecimal priceDelta;
    private Integer sortOrder;
}
