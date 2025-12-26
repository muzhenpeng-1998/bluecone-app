package com.bluecone.app.wechat.pay;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppService;
import com.bluecone.app.wechat.facade.pay.*;
import com.bluecone.app.wechat.pay.config.WeChatPayPartnerProperties;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.notify.WxPayPartnerNotifyV3Result;
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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * 微信支付 V3 服务商模式 Facade 实现（基于 WxJava）。
 * <p>
 * 这是项目中唯一使用 WxJava Pay SDK 的地方。
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "wechat.pay.partner", name = "enabled", havingValue = "true")
public class WxJavaWeChatPayPartnerFacade implements WeChatPayPartnerFacade {

    private static final Logger log = LoggerFactory.getLogger(WxJavaWeChatPayPartnerFacade.class);

    private final WxPayService wxPayService;
    private final WeChatPayPartnerProperties properties;
    private final WechatAuthorizedAppService wechatAuthorizedAppService;

    public WxJavaWeChatPayPartnerFacade(WxPayService wxPayService,
                                        WeChatPayPartnerProperties properties,
                                        WechatAuthorizedAppService wechatAuthorizedAppService) {
        this.wxPayService = wxPayService;
        this.properties = properties;
        this.wechatAuthorizedAppService = wechatAuthorizedAppService;
    }

    @Override
    public WeChatPartnerJsapiPrepayResult jsapiPrepay(WeChatPartnerJsapiPrepayCommand cmd) {
        log.info("[WxJavaWeChatPayPartnerFacade] 开始服务商 JSAPI 预下单，tenantId={}, outTradeNo={}",
                cmd.getTenantId(), cmd.getOutTradeNo());

        // 1. 校验必填参数
        validatePrepayCommand(cmd);

        // 2. 获取子商户小程序 AppID
        String subAppId = wechatAuthorizedAppService.getAuthorizerAppIdByTenantId(cmd.getTenantId())
                .orElseThrow(() -> {
                    log.error("[WxJavaWeChatPayPartnerFacade] 租户未授权小程序，tenantId={}", cmd.getTenantId());
                    return new BusinessException(CommonErrorCode.BAD_REQUEST, "该租户未授权小程序，无法发起服务商支付");
                });

        log.info("[WxJavaWeChatPayPartnerFacade] 子商户信息：subMchId={}, subAppId={}",
                maskMchId(cmd.getSubMchId()), maskAppId(subAppId));

        // 3. 确定回调地址
        String notifyUrl = StringUtils.hasText(cmd.getNotifyUrl())
                ? cmd.getNotifyUrl()
                : properties.getDefaultNotifyUrl();

        if (!StringUtils.hasText(notifyUrl)) {
            log.error("[WxJavaWeChatPayPartnerFacade] 回调地址为空，tenantId={}", cmd.getTenantId());
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "回调地址配置缺失，无法发起支付");
        }

        // 4. 构造服务商下单请求
        WxPayPartnerUnifiedOrderV3Request wxRequest = buildPartnerOrderRequest(cmd, subAppId, notifyUrl);

        // 5. 调用微信支付服务商下单接口
        WxPayUnifiedOrderV3Result.JsapiResult jsapiResult;
        try {
            jsapiResult = wxPayService.createPartnerOrderV3(TradeTypeEnum.JSAPI, wxRequest);

            log.info("[WxJavaWeChatPayPartnerFacade] 服务商下单成功，outTradeNo={}, prepayId={}",
                    cmd.getOutTradeNo(), jsapiResult != null ? jsapiResult.getPackageValue() : "N/A");
        } catch (WxPayException e) {
            log.error("[WxJavaWeChatPayPartnerFacade] 服务商下单失败，outTradeNo={}, errCode={}, errMsg={}",
                    cmd.getOutTradeNo(), e.getErrCode(), e.getErrCodeDes(), e);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST,
                    "微信支付下单失败: " + e.getErrCodeDes());
        }

        // 6. 转换为响应对象
        return convertToResult(jsapiResult, subAppId);
    }

    @Override
    public WeChatPartnerPayNotifyParsed parsePayNotify(WeChatPayNotifyParseCommand cmd) {
        log.info("[WxJavaWeChatPayPartnerFacade] 开始解析微信支付回调，timestamp={}, nonce={}, serial={}",
                cmd.getTimestamp(), cmd.getNonce(), cmd.getSerial());

        try {
            // 1. 构造签名头
            SignatureHeader signatureHeader = new SignatureHeader();
            signatureHeader.setTimeStamp(cmd.getTimestamp());
            signatureHeader.setNonce(cmd.getNonce());
            signatureHeader.setSignature(cmd.getSignature());
            signatureHeader.setSerial(cmd.getSerial());

            // 2. 调用 WxJava 进行验签和解密（服务商模式）
            WxPayPartnerNotifyV3Result notifyResult = wxPayService.parsePartnerOrderNotifyV3Result(
                    cmd.getRawBody(), signatureHeader);

            log.info("[WxJavaWeChatPayPartnerFacade] 验签解密成功，outTradeNo={}, transactionId={}, tradeState={}",
                    notifyResult.getResult().getOutTradeNo(),
                    notifyResult.getResult().getTransactionId(),
                    notifyResult.getResult().getTradeState());

            // 3. 转换为 Facade DTO
            return convertToParsed(notifyResult, cmd.getRawBody());

        } catch (WxPayException e) {
            log.error("[WxJavaWeChatPayPartnerFacade] 验签或解密失败，errCode={}, errMsg={}",
                    e.getErrCode(), e.getErrCodeDes(), e);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR,
                    "微信支付回调验签失败: " + e.getErrCodeDes());
        }
    }

    @Override
    public WeChatPartnerOrderQueryResult queryPartnerOrder(WeChatPartnerOrderQueryCommand cmd) {
        log.info("[WxJavaWeChatPayPartnerFacade] 开始查询服务商订单，subMchId={}, outTradeNo={}, transactionId={}",
                maskMchId(cmd.getSubMchId()), cmd.getOutTradeNo(), cmd.getTransactionId());

        try {
            com.github.binarywang.wxpay.bean.request.WxPayPartnerOrderQueryV3Request wxRequest = 
                    new com.github.binarywang.wxpay.bean.request.WxPayPartnerOrderQueryV3Request();
            wxRequest.setSpMchId(properties.getSpMchId());
            wxRequest.setSubMchId(cmd.getSubMchId());
            
            if (StringUtils.hasText(cmd.getTransactionId())) {
                wxRequest.setTransactionId(cmd.getTransactionId());
            } else if (StringUtils.hasText(cmd.getOutTradeNo())) {
                wxRequest.setOutTradeNo(cmd.getOutTradeNo());
            } else {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "商户订单号和微信支付单号不能同时为空");
            }

            com.github.binarywang.wxpay.bean.result.WxPayPartnerOrderQueryV3Result wxResult = 
                    wxPayService.queryPartnerOrderV3(wxRequest);

            log.info("[WxJavaWeChatPayPartnerFacade] 查询订单成功，outTradeNo={}, tradeState={}",
                    wxResult.getOutTradeNo(), wxResult.getTradeState());

            return convertToQueryResult(wxResult);

        } catch (WxPayException e) {
            log.error("[WxJavaWeChatPayPartnerFacade] 查询订单失败，errCode={}, errMsg={}",
                    e.getErrCode(), e.getErrCodeDes(), e);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR,
                    "微信支付查询订单失败: " + e.getErrCodeDes());
        }
    }

    @Override
    public void closePartnerOrder(WeChatPartnerOrderCloseCommand cmd) {
        log.info("[WxJavaWeChatPayPartnerFacade] 开始关闭服务商订单，subMchId={}, outTradeNo={}",
                maskMchId(cmd.getSubMchId()), cmd.getOutTradeNo());

        try {
            com.github.binarywang.wxpay.bean.request.WxPayPartnerOrderCloseV3Request wxRequest = 
                    new com.github.binarywang.wxpay.bean.request.WxPayPartnerOrderCloseV3Request();
            wxRequest.setSpMchId(properties.getSpMchId());
            wxRequest.setSubMchId(cmd.getSubMchId());
            wxRequest.setOutTradeNo(cmd.getOutTradeNo());

            wxPayService.closePartnerOrderV3(wxRequest);

            log.info("[WxJavaWeChatPayPartnerFacade] 关闭订单成功，outTradeNo={}", cmd.getOutTradeNo());

        } catch (WxPayException e) {
            log.error("[WxJavaWeChatPayPartnerFacade] 关闭订单失败，errCode={}, errMsg={}",
                    e.getErrCode(), e.getErrCodeDes(), e);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR,
                    "微信支付关闭订单失败: " + e.getErrCodeDes());
        }
    }

    @Override
    public WeChatPartnerRefundResult refund(WeChatPartnerRefundCommand cmd) {
        log.warn("[WxJavaWeChatPayPartnerFacade] 服务商退款功能待实现，subMchId={}, outRefundNo={}",
                maskMchId(cmd.getSubMchId()), cmd.getOutRefundNo());
        throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, 
                "服务商退款功能待实现，请参考 WxJava SDK 文档完成实现");
    }

    @Override
    public WeChatPartnerRefundNotifyParsed parseRefundNotify(WeChatPayNotifyParseCommand cmd) {
        log.warn("[WxJavaWeChatPayPartnerFacade] 服务商退款回调解析功能待实现");
        throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, 
                "服务商退款回调解析功能待实现，请参考 WxJava SDK 文档完成实现");
    }

    /**
     * 校验预下单命令参数。
     */
    private void validatePrepayCommand(WeChatPartnerJsapiPrepayCommand cmd) {
        if (!StringUtils.hasText(cmd.getSubMchId())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "子商户号不能为空");
        }
        if (!StringUtils.hasText(cmd.getPayerSubOpenId())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "支付者 openid 不能为空");
        }
        if (!StringUtils.hasText(cmd.getOutTradeNo())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "商户订单号不能为空");
        }
        if (cmd.getAmountTotal() == null || cmd.getAmountTotal() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "支付金额必须大于 0");
        }
        if (cmd.getTenantId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "租户 ID 不能为空");
        }
    }

    /**
     * 构造服务商下单请求。
     */
    private WxPayPartnerUnifiedOrderV3Request buildPartnerOrderRequest(WeChatPartnerJsapiPrepayCommand cmd,
                                                                       String subAppId,
                                                                       String notifyUrl) {
        WxPayPartnerUnifiedOrderV3Request wxRequest = new WxPayPartnerUnifiedOrderV3Request();

        // 服务商信息
        wxRequest.setSpAppid(properties.getSpAppId());
        wxRequest.setSpMchId(properties.getSpMchId());

        // 子商户信息
        wxRequest.setSubAppid(subAppId);
        wxRequest.setSubMchId(cmd.getSubMchId());

        // 订单信息
        wxRequest.setDescription(cmd.getDescription());
        wxRequest.setOutTradeNo(cmd.getOutTradeNo());
        wxRequest.setNotifyUrl(notifyUrl);

        // 金额信息（Long -> int，注意范围）
        WxPayPartnerUnifiedOrderV3Request.Amount amount = new WxPayPartnerUnifiedOrderV3Request.Amount();
        if (cmd.getAmountTotal() > Integer.MAX_VALUE || cmd.getAmountTotal() < 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "支付金额超出范围");
        }
        amount.setTotal(cmd.getAmountTotal().intValue());
        amount.setCurrency(StringUtils.hasText(cmd.getCurrency()) ? cmd.getCurrency() : "CNY");
        wxRequest.setAmount(amount);

        // 支付者信息（必须是子商户小程序的 openid）
        WxPayPartnerUnifiedOrderV3Request.Payer payer = new WxPayPartnerUnifiedOrderV3Request.Payer();
        payer.setSubOpenid(cmd.getPayerSubOpenId());
        wxRequest.setPayer(payer);

        // 附加数据
        if (StringUtils.hasText(cmd.getAttach())) {
            wxRequest.setAttach(cmd.getAttach());
        }

        return wxRequest;
    }

    /**
     * 转换为预下单结果。
     */
    private WeChatPartnerJsapiPrepayResult convertToResult(WxPayUnifiedOrderV3Result.JsapiResult jsapiResult,
                                                           String subAppId) {
        return WeChatPartnerJsapiPrepayResult.builder()
                .appId(subAppId)  // 注意：这里返回子商户小程序的 appId
                .timeStamp(jsapiResult.getTimeStamp())
                .nonceStr(jsapiResult.getNonceStr())
                .packageValue(jsapiResult.getPackageValue())
                .signType(jsapiResult.getSignType())
                .paySign(jsapiResult.getPaySign())
                .build();
    }

    /**
     * 转换为回调解析结果。
     */
    private WeChatPartnerPayNotifyParsed convertToParsed(WxPayPartnerNotifyV3Result notifyResult, String rawBody) {
        WxPayPartnerNotifyV3Result.DecryptNotifyResult result = notifyResult.getResult();

        WeChatPartnerPayNotifyParsed.WeChatPartnerPayNotifyParsedBuilder builder = WeChatPartnerPayNotifyParsed.builder()
                .rawBody(rawBody)
                .spAppId(result.getSpAppid())
                .spMchId(result.getSpMchid())
                .subAppId(result.getSubAppid())
                .subMchId(result.getSubMchid())
                .outTradeNo(result.getOutTradeNo())
                .transactionId(result.getTransactionId())
                .tradeState(result.getTradeState())
                .bankType(result.getBankType())
                .attach(result.getAttach());

        // 金额信息
        if (result.getAmount() != null) {
            builder.totalAmount(Long.valueOf(result.getAmount().getTotal()));
            builder.currency(result.getAmount().getCurrency());
        }

        // 支付者信息
        if (result.getPayer() != null) {
            builder.payerSubOpenId(result.getPayer().getSubOpenid());
        }

        // 支付成功时间
        if (result.getSuccessTime() != null) {
            try {
                // 微信返回格式：2018-06-08T10:34:56+08:00
                builder.successTime(OffsetDateTime.parse(result.getSuccessTime(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
            } catch (Exception e) {
                log.warn("[WxJavaWeChatPayPartnerFacade] 解析支付成功时间失败，successTime={}",
                        result.getSuccessTime(), e);
            }
        }

        // 预留字段
        builder.extraFields(new HashMap<>());

        log.info("[WxJavaWeChatPayPartnerFacade] 回调解析完成，spMchid={}, subMchid={}, subAppid={}, outTradeNo={}, transactionId={}",
                maskMchId(result.getSpMchid()), maskMchId(result.getSubMchid()),
                maskAppId(result.getSubAppid()), result.getOutTradeNo(), result.getTransactionId());

        return builder.build();
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

    /**
     * 转换为订单查询结果。
     */
    private WeChatPartnerOrderQueryResult convertToQueryResult(
            com.github.binarywang.wxpay.bean.result.WxPayPartnerOrderQueryV3Result wxResult) {
        
        WeChatPartnerOrderQueryResult.WeChatPartnerOrderQueryResultBuilder builder = 
                WeChatPartnerOrderQueryResult.builder()
                .spAppId(wxResult.getSpAppid())
                .spMchId(wxResult.getSpMchId())
                .subAppId(wxResult.getSubAppid())
                .subMchId(wxResult.getSubMchId())
                .outTradeNo(wxResult.getOutTradeNo())
                .transactionId(wxResult.getTransactionId())
                .tradeState(wxResult.getTradeState())
                .tradeStateDesc(wxResult.getTradeStateDesc())
                .attach(wxResult.getAttach());

        // 金额信息
        if (wxResult.getAmount() != null) {
            builder.totalAmount(Long.valueOf(wxResult.getAmount().getTotal()));
            builder.currency(wxResult.getAmount().getCurrency());
            if (wxResult.getAmount().getPayerTotal() != null) {
                builder.payerAmount(Long.valueOf(wxResult.getAmount().getPayerTotal()));
            }
        }

        // 支付成功时间
        if (wxResult.getSuccessTime() != null) {
            try {
                builder.successTime(OffsetDateTime.parse(wxResult.getSuccessTime(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
            } catch (Exception e) {
                log.warn("[WxJavaWeChatPayPartnerFacade] 解析支付成功时间失败，successTime={}",
                        wxResult.getSuccessTime(), e);
            }
        }

        return builder.build();
    }

}

