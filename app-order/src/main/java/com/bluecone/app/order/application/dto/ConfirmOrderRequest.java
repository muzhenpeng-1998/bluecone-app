package com.bluecone.app.order.application.dto;

import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认订单请求 DTO（应用层入口使用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderRequest {

    private Long tenantId;

    private Long storeId;

    private Long userId;

    private Long orderId;

    private String clientOrderNo;

    private String sessionId;

    private Integer sessionVersion;

    private Long version;

    private String payChannel;

    private BigDecimal payAmount;

    private String currency;

    private String remark;

    private String extJson;

    private String channel;

    private String bizType;

    private String orderSource;

    private BigDecimal clientTotalAmount;

    private BigDecimal clientDiscountAmount;

    private BigDecimal clientPayableAmount;

    private List<ConfirmOrderItemDTO> items;
}
