package com.bluecone.app.payment.infrastructure.gateway.wechat;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfig;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfigRepository;
import com.bluecone.app.payment.domain.channel.PaymentChannelType;
import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import com.bluecone.app.payment.domain.gateway.channel.ChannelPrepayCommand;
import com.bluecone.app.payment.domain.gateway.channel.ChannelPrepayResult;
import com.bluecone.app.payment.domain.gateway.channel.PaymentChannelGateway;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.wechat.facade.pay.WeChatPartnerJsapiPrepayCommand;
import com.bluecone.app.wechat.facade.pay.WeChatPartnerJsapiPrepayResult;
import com.bluecone.app.wechat.facade.pay.WeChatPayPartnerFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信 JSAPI 渠道网关适配器：实现通用 SPI，内部委托 WeChatPayPartnerFacade。
 */
@Component
public class WechatJsapiChannelGateway implements PaymentChannelGateway {

    private static final Logger log = LoggerFactory.getLogger(WechatJsapiChannelGateway.class);

    private final PaymentChannelConfigRepository paymentChannelConfigRepository;

    @Autowired(required = false)
    private WeChatPayPartnerFacade weChatPayPartnerFacade;

    public WechatJsapiChannelGateway(PaymentChannelConfigRepository paymentChannelConfigRepository) {
        this.paymentChannelConfigRepository = paymentChannelConfigRepository;
    }

    @Override
    public boolean supports(PaymentChannel channel, PaymentMethod method) {
        return channel == PaymentChannel.WECHAT && method == PaymentMethod.WECHAT_JSAPI;
    }

    @Override
    public ChannelPrepayResult prepay(ChannelPrepayCommand command) {
        if (weChatPayPartnerFacade == null) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR,
                    "微信支付服务未启用，无法发起支付（请检查 wechat.pay.partner.enabled 配置）");
        }

        PaymentOrder paymentOrder = command.getPaymentOrder();
        PaymentChannelType channelType = PaymentChannelType.fromChannelAndMethod(
                paymentOrder.getChannel(), paymentOrder.getMethod());
        if (channelType != PaymentChannelType.WECHAT_JSAPI) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "WechatJsapiChannelGateway 只支持 WECHAT_JSAPI");
        }

        // 1. 查询渠道配置
        PaymentChannelConfig config = paymentChannelConfigRepository.findByTenantStoreAndChannel(
                        paymentOrder.getTenantId(),
                        paymentOrder.getStoreId(),
                        channelType)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST, "微信 JSAPI 渠道未配置"));

        // 2. 校验配置
        if (!config.isEnabled()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "微信 JSAPI 渠道未启用");
        }
        if (config.getWeChatSecrets() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "微信支付配置为空");
        }
        String subMchId = config.getWeChatSecrets().getSubMchId();
        if (!StringUtils.hasText(subMchId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "子商户号配置缺失");
        }

        // 3. 计算金额（元 -> 分）
        BigDecimal payable = paymentOrder.getPayableAmount();
        if (payable == null) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "支付单缺少应付金额");
        }
        long fen = payable.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        // 4. 构造 Facade 命令
        WeChatPartnerJsapiPrepayCommand facadeCmd = WeChatPartnerJsapiPrepayCommand.builder()
                .tenantId(paymentOrder.getTenantId())
                .subMchId(subMchId)
                .payerSubOpenId(command.getPayerOpenId())
                .outTradeNo(String.valueOf(paymentOrder.getId()))
                .amountTotal(fen)
                .currency(paymentOrder.getCurrency())
                .description(command.getDescription())
                .attach(command.getAttach())
                .notifyUrl(config.getNotifyUrl())  // 从配置中取回调地址
                .build();

        // 5. 调用 Facade 预下单
        WeChatPartnerJsapiPrepayResult result = weChatPayPartnerFacade.jsapiPrepay(facadeCmd);

        // 6. 构造渠道上下文（供前端唤起支付）
        Map<String, Object> context = new HashMap<>();
        context.put("appId", result.getAppId());
        context.put("timeStamp", result.getTimeStamp());
        context.put("nonceStr", result.getNonceStr());
        context.put("package", result.getPackageValue());
        context.put("signType", result.getSignType());
        context.put("paySign", result.getPaySign());

        log.info("[WechatJsapiGateway] prepay success paymentId={} prepayPackage={} tenantId={} storeId={}",
                paymentOrder.getId(), result.getPackageValue(), paymentOrder.getTenantId(), paymentOrder.getStoreId());

        return new ChannelPrepayResult(result.getPackageValue(), context);
    }
}
