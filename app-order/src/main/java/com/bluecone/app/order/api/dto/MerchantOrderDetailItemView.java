package com.bluecone.app.order.api.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 商户侧订单详情明细。
 */
@Data
public class MerchantOrderDetailItemView {

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
