package com.bluecone.app.payment.domain.gateway.channel;

import com.bluecone.app.payment.domain.model.PaymentOrder;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 通用的渠道预下单命令，聚合了支付单与渠道侧必要信息。
 */
public class ChannelPrepayCommand {

    private final PaymentOrder paymentOrder;
    private final String payerOpenId;
    private final String description;
    private final String attach;

    public ChannelPrepayCommand(final PaymentOrder paymentOrder,
                                final String payerOpenId,
                                final String description,
                                final String attach) {
        this.paymentOrder = Objects.requireNonNull(paymentOrder, "paymentOrder must not be null");
        this.payerOpenId = payerOpenId;
        this.description = description;
        this.attach = attach;
    }

    public PaymentOrder getPaymentOrder() {
        return paymentOrder;
    }

    public String getPayerOpenId() {
        return payerOpenId;
    }

    public String getDescription() {
        return description;
    }

    public String getAttach() {
        return attach;
    }

    /**
     * 便捷获取应付金额（元）。
     */
    public BigDecimal getPayableAmount() {
        return paymentOrder.getPayableAmount();
    }
}
