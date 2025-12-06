package com.bluecone.app.payment.domain.gateway;

import lombok.Data;

/**
 * 微信 JSAPI 预下单请求。
 */
@Data
public class WeChatJsapiPrepayRequest {

    private Long paymentOrderId;

    private Long tenantId;

    private Long storeId;

    private Long userId;

    /**
     * 金额（单位分）。
     */
    private Long amountTotal;

    private String currency;

    private String description;

    private String outTradeNo;

    private String payerOpenId;

    private String attach;
}
