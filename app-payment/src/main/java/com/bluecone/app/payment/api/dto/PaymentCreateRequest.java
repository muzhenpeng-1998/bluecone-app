package com.bluecone.app.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付创建请求（M1）。
 * <p>用于创建支付单并返回预支付信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {

    /**
     * 租户ID（必填）。
     */
    private Long tenantId;

    /**
     * 门店ID（必填）。
     */
    private Long storeId;

    /**
     * 订单ID（必填）。
     */
    private Long orderId;

    /**
     * 支付渠道（必填）：WECHAT_JSAPI、WECHAT_NATIVE、ALIPAY_WAP、MOCK等。
     */
    private String channel;

    /**
     * 用户ID（可选，用于某些渠道的用户标识）。
     */
    private Long userId;

    /**
     * 客户端IP（可选）。
     */
    private String clientIp;
}
