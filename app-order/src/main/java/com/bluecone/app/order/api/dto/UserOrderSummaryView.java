package com.bluecone.app.order.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户订单列表摘要视图。
 */
@Data
public class UserOrderSummaryView {

    private Long orderId;

    private String orderNo;

    private LocalDateTime createdAt;

    private String status;

    private String payStatus;

    private BigDecimal payableAmount;

    private String currency;

    private String orderSource;

    private String storeName;

    private String firstItemName;

    private Integer totalItemCount;

    private Boolean userDeleted;
}
