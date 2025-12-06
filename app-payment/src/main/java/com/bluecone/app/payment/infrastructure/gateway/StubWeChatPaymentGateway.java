package com.bluecone.app.payment.infrastructure.gateway;

import com.bluecone.app.payment.domain.channel.PaymentChannelConfig;
import com.bluecone.app.payment.domain.gateway.WeChatJsapiPrepayRequest;
import com.bluecone.app.payment.domain.gateway.WeChatJsapiPrepayResponse;
import com.bluecone.app.payment.domain.gateway.WeChatPaymentGateway;
import org.springframework.stereotype.Component;

/**
 * 微信支付网关的占位实现：
 * <p>
 * - 仅用于当前阶段解决 Bean 注入问题，便于应用启动和后续联调；<br>
 * - 不调用真实微信接口，返回最小的占位参数；<br>
 * - 后续需要接入官方 SDK 时，可替换/扩展为正式实现。
 */
@Component
public class StubWeChatPaymentGateway implements WeChatPaymentGateway {

    @Override
    public WeChatJsapiPrepayResponse jsapiPrepay(WeChatJsapiPrepayRequest request, PaymentChannelConfig config) {
        WeChatJsapiPrepayResponse resp = new WeChatJsapiPrepayResponse();
        resp.setAppId(config == null ? null : config.getWeChatSecrets() != null ? config.getWeChatSecrets().getAppId() : null);
        resp.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        resp.setNonceStr("stub-nonce");
        resp.setPackageValue("prepay_id=stub");
        resp.setSignType("RSA");
        resp.setPaySign("stub-sign");
        return resp;
    }
}
