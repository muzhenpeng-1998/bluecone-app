package com.bluecone.app.inventory.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseStockCommand {

    private Long tenantId;

    private Long storeId;

    private Long orderId;

    private List<Long> lockIds;

    private String requestId;

    /**
     * 是否为超时释放。
     */
    private boolean expired;

    /**
     * 释放原因，默认 ORDER_CANCEL，可结合 expired 转化为 PAYMENT_TIMEOUT 等。
     */
    private String reason;
}
