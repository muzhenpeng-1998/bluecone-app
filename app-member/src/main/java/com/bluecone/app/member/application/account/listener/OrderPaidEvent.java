package com.bluecone.app.user.application.account.listener;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付完成事件占位。
 */
@Data
public class OrderPaidEvent {

    private Long tenantId;

    private Long storeId;

    private Long memberId;

    private Long userId;

    private String orderNo;

    private BigDecimal payAmount;

    private String orderType;

    private LocalDateTime paidAt;
}
