package com.bluecone.app.payment.domain.gateway.channel;

import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;

/**
 * 多支付渠道统一的预下单 SPI。
 */
public interface PaymentChannelGateway {

    /**
     * 当前网关是否支持指定的渠道 + 方式组合。
     */
    boolean supports(PaymentChannel channel, PaymentMethod method);

    /**
     * 执行渠道预下单，返回渠道上下文供前端唤起。
     */
    ChannelPrepayResult prepay(ChannelPrepayCommand command);
}
