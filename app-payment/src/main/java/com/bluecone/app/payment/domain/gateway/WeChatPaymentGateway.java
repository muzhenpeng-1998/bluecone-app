package com.bluecone.app.payment.domain.gateway;

import com.bluecone.app.payment.domain.channel.PaymentChannelConfig;

/**
 * 微信支付网关接口（骨架）。
 * <p>后续在基础设施层实现对接微信官方 SDK。</p>
 */
public interface WeChatPaymentGateway {

    /**
     * 微信 JSAPI 预下单。
     *
     * @param request 预下单请求
     * @param config  渠道配置（由上层查出并传入）
     * @return 前端唤起支付所需参数
     */
    WeChatJsapiPrepayResponse jsapiPrepay(WeChatJsapiPrepayRequest request, PaymentChannelConfig config);
}
