package com.bluecone.app.payment.infrastructure.wechatpay;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppService;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfig;
import com.bluecone.app.payment.domain.channel.PaymentChannelType;
import com.bluecone.app.payment.domain.gateway.WeChatJsapiPrepayRequest;
import com.bluecone.app.payment.domain.gateway.WeChatJsapiPrepayResponse;
import com.bluecone.app.payment.domain.gateway.WeChatPaymentGateway;
import com.github.binarywang.wxpay.bean.request.WxPayPartnerUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信支付 V3 服务商模式网关实现（基于 WxJava）。
 * <p>
 * 实现服务商为多个子商户小程序发起 JSAPI 支付的能力。
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "bluecone.wechat.pay", name = "enabled", havingValue = "true")
public class WxJavaWeChatPaymentGateway implements WeChatPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(WxJavaWeChatPaymentGateway.class);

    private final WxPayService wxPayService;
    private final BlueconeWeChatPayProperties properties;
    private final WechatAuthorizedAppService wechatAuthorizedAppService;

    public WxJavaWeChatPaymentGateway(WxPayService wxPayService,
                                      BlueconeWeChatPayProperties properties,
                                      WechatAuthorizedAppService wechatAuthorizedAppService) {
        this.wxPayService = wxPayService;
        this.properties = properties;
        this.wechatAuthorizedAppService = wechatAuthorizedAppService;
    }

    @Override
    public WeChatJsapiPrepayResponse jsapiPrepay(WeChatJsapiPrepayRequest request, PaymentChannelConfig config) {
        log.info("[WxJavaWeChatPaymentGateway] 开始服务商 JSAPI 预下单，tenantId={}, paymentOrderId={}, outTradeNo={}",
                request.getTenantId(), request.getPaymentOrderId(), request.getOutTradeNo());

        // 1. 校验渠道配置
        validateChannelConfig(config);

        // 2. 获取子商户号
        String subMchId = config.getWeChatSecrets().getSubMchId();
        if (!StringUtils.hasText(subMchId)) {
            log.error("[WxJavaWeChatPaymentGateway] 子商户号为空，tenantId={}", request.getTenantId());
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "子商户号配置缺失，无法发起服务商支付");
        }

        // 3. 获取子商户小程序 AppID
        String subAppId = wechatAuthorizedAppService.getAuthorizerAppIdByTenantId(request.getTenantId())
                .orElseThrow(() -> {
                    log.error("[WxJavaWeChatPaymentGateway] 租户未授权小程序，tenantId={}", request.getTenantId());
                    return new BusinessException(CommonErrorCode.BAD_REQUEST, "该租户未授权小程序，无法发起服务商支付");
                });

        log.info("[WxJavaWeChatPaymentGateway] 子商户信息：subMchId={}, subAppId={}", maskMchId(subMchId), maskAppId(subAppId));

        // 4. 确定回调地址
        String notifyUrl = StringUtils.hasText(config.getNotifyUrl())
                ? config.getNotifyUrl()
                : properties.getDefaultNotifyUrl();

        if (!StringUtils.hasText(notifyUrl)) {
            log.error("[WxJavaWeChatPaymentGateway] 回调地址为空，tenantId={}", request.getTenantId());
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "回调地址配置缺失，无法发起支付");
        }

        // 5. 构造服务商下单请求
        WxPayPartnerUnifiedOrderV3Request wxRequest = buildPartnerOrderRequest(request, subMchId, subAppId, notifyUrl);

        // 6. 调用微信支付服务商下单接口
        WxPayUnifiedOrderV3Result.JsapiResult jsapiResult;
        try {
            // 使用服务商模式下单
            jsapiResult = wxPayService.createPartnerOrderV3(TradeTypeEnum.JSAPI, wxRequest);
            
            log.info("[WxJavaWeChatPaymentGateway] 服务商下单成功，outTradeNo={}, prepayId={}",
                    request.getOutTradeNo(), jsapiResult != null ? jsapiResult.getPackageValue() : "N/A");
        } catch (WxPayException e) {
            log.error("[WxJavaWeChatPaymentGateway] 服务商下单失败，outTradeNo={}, errCode={}, errMsg={}",
                    request.getOutTradeNo(), e.getErrCode(), e.getErrCodeDes(), e);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST,
                    "微信支付下单失败: " + e.getErrCodeDes());
        }

        // 7. 转换为响应对象
        return convertToResponse(jsapiResult, subAppId);
    }

    /**
     * 校验渠道配置。
     */
    private void validateChannelConfig(PaymentChannelConfig config) {
        if (config == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "支付渠道配置为空");
        }
        if (!config.isEnabled()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "支付渠道未启用");
        }
        if (config.getChannelType() != PaymentChannelType.WECHAT_JSAPI) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "支付渠道类型不匹配，期望 WECHAT_JSAPI");
        }
        if (config.getWeChatSecrets() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "微信支付配置为空");
        }
    }

    /**
     * 构造服务商下单请求。
     */
    private WxPayPartnerUnifiedOrderV3Request buildPartnerOrderRequest(WeChatJsapiPrepayRequest request,
                                                                       String subMchId,
                                                                       String subAppId,
                                                                       String notifyUrl) {
        WxPayPartnerUnifiedOrderV3Request wxRequest = new WxPayPartnerUnifiedOrderV3Request();

        // 服务商信息
        wxRequest.setSpAppid(properties.getSpAppId());
        wxRequest.setSpMchId(properties.getSpMchId());

        // 子商户信息
        wxRequest.setSubAppid(subAppId);
        wxRequest.setSubMchId(subMchId);

        // 订单信息
        wxRequest.setDescription(request.getDescription());
        wxRequest.setOutTradeNo(request.getOutTradeNo());
        wxRequest.setNotifyUrl(notifyUrl);

        // 金额信息（Long -> int，注意范围）
        WxPayPartnerUnifiedOrderV3Request.Amount amount = new WxPayPartnerUnifiedOrderV3Request.Amount();
        if (request.getAmountTotal() != null) {
            if (request.getAmountTotal() > Integer.MAX_VALUE || request.getAmountTotal() < 0) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "支付金额超出范围");
            }
            amount.setTotal(request.getAmountTotal().intValue());
        }
        amount.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : "CNY");
        wxRequest.setAmount(amount);

        // 支付者信息（必须是子商户小程序的 openid）
        WxPayPartnerUnifiedOrderV3Request.Payer payer = new WxPayPartnerUnifiedOrderV3Request.Payer();
        payer.setSubOpenid(request.getPayerOpenId());
        wxRequest.setPayer(payer);

        // 附加数据
        if (StringUtils.hasText(request.getAttach())) {
            wxRequest.setAttach(request.getAttach());
        }

        return wxRequest;
    }

    /**
     * 转换为响应对象。
     */
    private WeChatJsapiPrepayResponse convertToResponse(WxPayUnifiedOrderV3Result.JsapiResult jsapiResult, String subAppId) {
        WeChatJsapiPrepayResponse response = new WeChatJsapiPrepayResponse();
        response.setAppId(subAppId);  // 注意：这里返回子商户小程序的 appId
        response.setTimeStamp(jsapiResult.getTimeStamp());
        response.setNonceStr(jsapiResult.getNonceStr());
        response.setPackageValue(jsapiResult.getPackageValue());
        response.setSignType(jsapiResult.getSignType());
        response.setPaySign(jsapiResult.getPaySign());
        return response;
    }

    /**
     * 脱敏商户号（显示前4位和后4位）。
     */
    private String maskMchId(String mchId) {
        if (mchId == null || mchId.length() <= 8) {
            return "***";
        }
        return mchId.substring(0, 4) + "***" + mchId.substring(mchId.length() - 4);
    }

    /**
     * 脱敏 AppID（显示前6位和后4位）。
     */
    private String maskAppId(String appId) {
        if (appId == null || appId.length() <= 10) {
            return "***";
        }
        return appId.substring(0, 6) + "***" + appId.substring(appId.length() - 4);
    }
}

