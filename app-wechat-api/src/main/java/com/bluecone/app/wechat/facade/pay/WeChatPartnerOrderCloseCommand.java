package com.bluecone.app.wechat.facade.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付服务商订单关单命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPartnerOrderCloseCommand {

    /**
     * 子商户号（sub_mchid）。
     */
    private String subMchId;

    /**
     * 商户订单号（out_trade_no）。
     */
    private String outTradeNo;
}

