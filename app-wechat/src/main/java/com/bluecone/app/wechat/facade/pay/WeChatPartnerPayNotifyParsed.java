package com.bluecone.app.wechat.facade.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 微信支付服务商模式回调解析结果。
 * <p>
 * 包含验签解密后的回调数据。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPartnerPayNotifyParsed {

    /**
     * 原始报文（用于日志或审计）。
     */
    private String rawBody;

    /**
     * 服务商应用 ID（sp_appid）。
     */
    private String spAppId;

    /**
     * 服务商商户号（sp_mchid）。
     */
    private String spMchId;

    /**
     * 子商户应用 ID（sub_appid）。
     */
    private String subAppId;

    /**
     * 子商户号（sub_mchid）。
     */
    private String subMchId;

    /**
     * 商户订单号（out_trade_no）。
     */
    private String outTradeNo;

    /**
     * 微信支付单号（transaction_id）。
     */
    private String transactionId;

    /**
     * 交易状态（SUCCESS / REFUND / NOTPAY / CLOSED 等）。
     */
    private String tradeState;

    /**
     * 总金额（单位：分）。
     */
    private Long totalAmount;

    /**
     * 货币类型（如 CNY）。
     */
    private String currency;

    /**
     * 银行类型（可选）。
     */
    private String bankType;

    /**
     * 附加数据（attach）。
     */
    private String attach;

    /**
     * 支付者 openid（服务商模式下为 sub_openid）。
     */
    private String payerSubOpenId;

    /**
     * 支付成功时间。
     */
    private Instant successTime;

    /**
     * 预留字段，用于承载其它通知字段。
     */
    private Map<String, Object> extraFields;
}

