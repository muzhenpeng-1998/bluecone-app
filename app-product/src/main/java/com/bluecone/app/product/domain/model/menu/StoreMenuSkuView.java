package com.bluecone.app.product.domain.model.menu;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单 SKU 视图，包含定价与扩展信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuSkuView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long skuId;
    private String name;
    private BigDecimal price;
    private BigDecimal originPrice;
    private Boolean defaultSku;
    private Map<String, Object> ext;
}
