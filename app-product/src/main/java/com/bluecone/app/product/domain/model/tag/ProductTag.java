package com.bluecone.app.product.domain.model.tag;

import com.bluecone.app.product.domain.enums.ProductStatus;
import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品运营标签的领域模型，对应“新品/热销/低卡”等前台展示标签。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTag implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private String name;

    /**
     * 标签样式配置，通常包含颜色、图标、文案等。
     */
    private Map<String, Object> style;

    private ProductStatus status;

    private Integer sortOrder;
}
