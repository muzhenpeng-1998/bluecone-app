package com.bluecone.app.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付回调响应（M1）。
 * <p>返回处理结果，用于告知渠道是否成功。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotifyResponse {

    /**
     * 是否处理成功。
     */
    private Boolean success;

    /**
     * 响应消息。
     */
    private String message;

    /**
     * 是否为幂等返回（重复通知）。
     */
    private Boolean idempotent;
}
