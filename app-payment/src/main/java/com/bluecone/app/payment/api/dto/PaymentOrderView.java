package com.bluecone.app.payment.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 支付单查询视图对象。
 */
@Data
public class PaymentOrderView {

    private Long id;

    private String paymentNo;

    private Long tenantId;

    private Long storeId;

    private String bizType;

    private String bizOrderNo;

    private String channelCode;

    private String methodCode;

    private String sceneCode;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private BigDecimal paidAmount;

    private String currency;

    private String status;

    private LocalDateTime expireAt;

    private LocalDateTime paidAt;

    private String channelTradeNo;

    private Map<String, Object> channelContext;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
