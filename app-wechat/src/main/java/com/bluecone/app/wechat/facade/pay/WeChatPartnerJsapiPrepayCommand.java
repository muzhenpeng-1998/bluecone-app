package com.bluecone.app.wechat.facade.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付服务商 JSAPI 预下单命令。
 * <p>
 * 仅包含基础类型，不依赖支付域对象或 WxJava 类型。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPartnerJsapiPrepayCommand {

    /**
     * 租户 ID（用于查询子商户小程序 appId）。
     */
    private Long tenantId;

    /**
     * 子商户号（sub_mchid）。
     */
    private String subMchId;

    /**
     * 支付者 openid（必须是子商户小程序的 openid）。
     */
    private String payerSubOpenId;

    /**
     * 商户订单号（out_trade_no）。
     */
    private String outTradeNo;

    /**
     * 订单金额（单位：分）。
     */
    private Long amountTotal;

    /**
     * 货币类型（默认 CNY）。
     */
    private String currency;

    /**
     * 商品描述。
     */
    private String description;

    /**
     * 附加数据（可选）。
     */
    private String attach;

    /**
     * 回调地址（可选，为空时使用默认配置）。
     */
    private String notifyUrl;
}

