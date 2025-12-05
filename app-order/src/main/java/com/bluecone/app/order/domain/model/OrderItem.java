package com.bluecone.app.order.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long tenantId;

    private Long storeId;

    // 商品维度
    private Long productId;

    private Long skuId;

    private String productName;

    private String skuName;

    private String productCode;

    // 数量 & 金额
    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    // 属性 & 备注
    @Builder.Default
    private Map<String, Object> attrs = Collections.emptyMap();

    private String remark;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;
}
