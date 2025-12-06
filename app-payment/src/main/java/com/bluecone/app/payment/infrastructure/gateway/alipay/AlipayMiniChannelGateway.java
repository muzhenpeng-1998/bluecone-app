package com.bluecone.app.payment.infrastructure.gateway.alipay;

import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import com.bluecone.app.payment.domain.gateway.channel.ChannelPrepayCommand;
import com.bluecone.app.payment.domain.gateway.channel.ChannelPrepayResult;
import com.bluecone.app.payment.domain.gateway.channel.PaymentChannelGateway;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝小程序预下单骨架，实现通用渠道 SPI（暂未接入真实 SDK）。
 */
@Component
public class AlipayMiniChannelGateway implements PaymentChannelGateway {

    private static final Logger log = LoggerFactory.getLogger(AlipayMiniChannelGateway.class);

    @Override
    public boolean supports(PaymentChannel channel, PaymentMethod method) {
        return channel == PaymentChannel.ALIPAY && method == PaymentMethod.ALIPAY_MINI;
    }

    @Override
    public ChannelPrepayResult prepay(ChannelPrepayCommand command) {
        PaymentOrder paymentOrder = command.getPaymentOrder();
        log.info("[AlipayMini] prepay skeleton invoked paymentId={} amount={} tenantId={} storeId={}",
                paymentOrder.getId(), command.getPayableAmount(), paymentOrder.getTenantId(), paymentOrder.getStoreId());

        // TODO: 接入支付宝小程序 SDK，生成真实的 tradeNo/orderStr 并填充参数。
        Map<String, Object> context = new HashMap<>();
        context.put("channel", "ALIPAY");
        context.put("mock", true);
        context.put("message", "Alipay Mini prepay not implemented yet");

        return new ChannelPrepayResult("ALIPAY-MINI-MOCK-" + paymentOrder.getId(), context);
    }
}
