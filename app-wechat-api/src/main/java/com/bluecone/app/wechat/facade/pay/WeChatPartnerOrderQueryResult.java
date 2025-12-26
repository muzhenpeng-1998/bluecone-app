package com.bluecone.app.wechat.facade.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 微信支付服务商订单查询结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPartnerOrderQueryResult {

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
     * 交易状态（SUCCESS / REFUND / NOTPAY / CLOSED / REVOKED / USERPAYING / PAYERROR）。
     */
    private String tradeState;

    /**
     * 交易状态描述。
     */
    private String tradeStateDesc;

    /**
     * 总金额（单位：分）。
     */
    private Long totalAmount;

    /**
     * 货币类型（如 CNY）。
     */
    private String currency;

    /**
     * 用户实际支付金额（单位：分）。
     */
    private Long payerAmount;

    /**
     * 支付完成时间。
     */
    private Instant successTime;

    /**
     * 附加数据（attach）。
     */
    private String attach;
}

