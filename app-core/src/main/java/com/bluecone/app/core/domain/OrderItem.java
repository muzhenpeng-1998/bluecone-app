package com.bluecone.app.core.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细领域模型
 */
@Data
@Builder
public class OrderItem {

    private Long id;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}
