package com.bluecone.app.payment.api.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Data;

/**
 * 支付单创建结果 DTO。
 */
@Data
public class CreatePaymentResult {

    private Long paymentId;

    private String paymentNo;   // 如无专用字段暂用 paymentId 字符串

    private String bizType;

    private String bizOrderNo;

    private String channelCode;

    private String methodCode;

    private String sceneCode;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private String status;

    /**
     * 前端发起支付所需的渠道上下文（预留字段，当前可为空）。
     */
    private Map<String, Object> channelContext;
}
