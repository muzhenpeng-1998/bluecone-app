package com.bluecone.app.order.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 商户侧订单详情视图。
 */
@Data
public class MerchantOrderDetailView {

    private Long orderId;
    private String orderNo;

    private Long tenantId;
    private Long storeId;
    private Long userId;

    private String status;
    private String payStatus;

    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private String currency;

    private String orderSource;
    private String channel;

    private String userNickname;
    private String userMobileMasked;

    private String storeName;
    private String storeAddress;

    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime payTime;
    private LocalDateTime completedAt;

    private String deliveryType;
    private String deliveryStatus;
    private String receiverName;
    private String receiverMobileMasked;
    private String receiverAddress;

    private String refundStatus;
    private BigDecimal refundedAmount;

    private List<MerchantOrderDetailItemView> items;
}
