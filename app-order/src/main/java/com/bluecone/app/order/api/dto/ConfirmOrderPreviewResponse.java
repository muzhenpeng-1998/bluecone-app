package com.bluecone.app.order.api.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序用户侧确认订单预览响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderPreviewResponse {

    private boolean canPlaceOrder;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private String currency;

    private Integer expectedReadyTimeSeconds;

    private String storeOpenStatus;

    private String message;

    private Integer sessionVersion;

    private String ext;
}
