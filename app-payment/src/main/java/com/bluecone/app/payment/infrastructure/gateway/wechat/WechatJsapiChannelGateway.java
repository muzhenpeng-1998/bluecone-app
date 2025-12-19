package com.bluecone.app.payment.infrastructure.gateway.wechat;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfig;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfigRepository;
import com.bluecone.app.payment.domain.channel.PaymentChannelType;
import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import com.bluecone.app.payment.domain.gateway.WeChatJsapiPrepayRequest;
import com.bluecone.app.payment.domain.gateway.WeChatJsapiPrepayResponse;
import com.bluecone.app.payment.domain.gateway.WeChatPaymentGateway;
import com.bluecone.app.payment.domain.gateway.channel.ChannelPrepayCommand;
import com.bluecone.app.payment.domain.gateway.channel.ChannelPrepayResult;
import com.bluecone.app.payment.domain.gateway.channel.PaymentChannelGateway;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信 JSAPI 渠道网关适配器：实现通用 SPI，内部委托 WeChatPaymentGateway。
 */
@Component
public class WechatJsapiChannelGateway implements PaymentChannelGateway {

    private static final Logger log = LoggerFactory.getLogger(WechatJsapiChannelGateway.class);

    private final PaymentChannelConfigRepository paymentChannelConfigRepository;
    private final WeChatPaymentGateway weChatPaymentGateway;

    public WechatJsapiChannelGateway(PaymentChannelConfigRepository paymentChannelConfigRepository,
                                     WeChatPaymentGateway weChatPaymentGateway) {
        this.paymentChannelConfigRepository = paymentChannelConfigRepository;
        this.weChatPaymentGateway = weChatPaymentGateway;
    }

    @Override
    public boolean supports(PaymentChannel channel, PaymentMethod method) {
        return channel == PaymentChannel.WECHAT && method == PaymentMethod.WECHAT_JSAPI;
    }

    @Override
    public ChannelPrepayResult prepay(ChannelPrepayCommand command) {
        PaymentOrder paymentOrder = command.getPaymentOrder();
        PaymentChannelType channelType = PaymentChannelType.fromChannelAndMethod(
                paymentOrder.getChannel(), paymentOrder.getMethod());
        if (channelType != PaymentChannelType.WECHAT_JSAPI) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "WechatJsapiChannelGateway 只支持 WECHAT_JSAPI");
        }

        PaymentChannelConfig config = paymentChannelConfigRepository.findByTenantStoreAndChannel(
                        paymentOrder.getTenantId(),
                        paymentOrder.getStoreId(),
                        channelType)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST, "微信 JSAPI 渠道未配置"));

        BigDecimal payable = paymentOrder.getPayableAmount();
        if (payable == null) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "支付单缺少应付金额");
        }
        long fen = payable.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        WeChatJsapiPrepayRequest req = new WeChatJsapiPrepayRequest();
        req.setPaymentOrderId(paymentOrder.getId());
        req.setTenantId(paymentOrder.getTenantId());
        req.setStoreId(paymentOrder.getStoreId());
        req.setUserId(paymentOrder.getUserId());
        req.setAmountTotal(fen);
        req.setCurrency(paymentOrder.getCurrency());
        req.setDescription(command.getDescription());
        req.setOutTradeNo(String.valueOf(paymentOrder.getId()));
        req.setPayerOpenId(command.getPayerOpenId());
        req.setAttach(command.getAttach());

        WeChatJsapiPrepayResponse resp = weChatPaymentGateway.jsapiPrepay(req, config);
        Map<String, Object> context = new HashMap<>();
        context.put("appId", resp.getAppId());
        context.put("timeStamp", resp.getTimeStamp());
        context.put("nonceStr", resp.getNonceStr());
        context.put("package", resp.getPackageValue());
        context.put("signType", resp.getSignType());
        context.put("paySign", resp.getPaySign());

        log.info("[WechatJsapiGateway] prepay success paymentId={} prepayPackage={} tenantId={} storeId={}",
                paymentOrder.getId(), resp.getPackageValue(), paymentOrder.getTenantId(), paymentOrder.getStoreId());

        return new ChannelPrepayResult(resp.getPackageValue(), context);
    }
}
