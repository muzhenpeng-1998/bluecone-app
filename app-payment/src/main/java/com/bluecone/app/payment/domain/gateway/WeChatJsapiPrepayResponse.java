package com.bluecone.app.payment.domain.gateway;

import lombok.Data;

/**
 * 微信 JSAPI 预下单响应，封装前端唤起支付所需参数。
 */
@Data
public class WeChatJsapiPrepayResponse {

    private String appId;

    private String timeStamp;

    private String nonceStr;

    /**
     * 对应 "prepay_id=xxx"
     */
    private String packageValue;

    private String signType;

    private String paySign;
}
