package com.bluecone.app.order.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商户侧订单列表摘要。
 */
@Data
public class MerchantOrderSummaryView {

    private Long orderId;
    private String orderNo;

    private Long tenantId;
    private Long storeId;

    private LocalDateTime createdAt;

    private String status;

    private String payStatus;

    private BigDecimal payableAmount;

    private String currency;

    private String orderSource;

    private String channel;

    private String tableInfo;

    private String firstItemName;

    private Integer totalItemCount;

    private String userNickname;

    private String userMobileMasked;
}
