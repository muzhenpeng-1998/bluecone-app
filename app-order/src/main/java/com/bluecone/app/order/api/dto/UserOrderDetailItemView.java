package com.bluecone.app.order.api.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 用户订单详情中的明细项。
 */
@Data
public class UserOrderDetailItemView {

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuName;

    private String productCode;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotalAmount;

    private String attrs;

    private String remark;
}
