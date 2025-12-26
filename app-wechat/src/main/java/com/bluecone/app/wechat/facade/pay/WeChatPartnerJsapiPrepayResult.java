package com.bluecone.app.wechat.facade.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付服务商 JSAPI 预下单结果。
 * <p>
 * 包含前端唤起支付所需的参数。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPartnerJsapiPrepayResult {

    /**
     * 子商户小程序 appId。
     */
    private String appId;

    /**
     * 时间戳。
     */
    private String timeStamp;

    /**
     * 随机字符串。
     */
    private String nonceStr;

    /**
     * 订单详情扩展字符串（格式：prepay_id=xxx）。
     */
    private String packageValue;

    /**
     * 签名类型（默认 RSA）。
     */
    private String signType;

    /**
     * 签名。
     */
    private String paySign;
}

