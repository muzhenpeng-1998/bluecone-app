package com.bluecone.app.payment.domain.gateway.channel;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 根据渠道+方式选择合适的渠道网关实现。
 */
@Component
public class PaymentChannelGatewayRouter {

    private final List<PaymentChannelGateway> gateways;

    public PaymentChannelGatewayRouter(final List<PaymentChannelGateway> gateways) {
        this.gateways = Objects.requireNonNullElseGet(gateways, List::of);
    }

    public ChannelPrepayResult prepay(final PaymentChannel channel,
                                      final PaymentMethod method,
                                      final ChannelPrepayCommand command) {
        return gateways.stream()
                .filter(gw -> gw.supports(channel, method))
                .findFirst()
                .map(gw -> gw.prepay(command))
                .orElseThrow(() -> new BizException(
                        CommonErrorCode.BAD_REQUEST,
                        "未找到匹配的支付网关实现: channel=" + channel + ", method=" + method
                ));
    }
}
