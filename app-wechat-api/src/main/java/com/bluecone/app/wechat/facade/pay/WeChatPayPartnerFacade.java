package com.bluecone.app.wechat.facade.pay;

/**
 * 微信支付 V3 服务商模式 Facade 接口。
 * <p>
 * 对外暴露微信支付服务商能力，隐藏 WxJava SDK 实现细节。
 * </p>
 */
public interface WeChatPayPartnerFacade {

    /**
     * 服务商 JSAPI 预下单。
     *
     * @param command 预下单命令
     * @return 预下单结果（包含前端唤起支付所需参数）
     */
    WeChatPartnerJsapiPrepayResult jsapiPrepay(WeChatPartnerJsapiPrepayCommand command);

    /**
     * 解析微信支付 V3 服务商模式回调通知。
     *
     * @param command 回调解析命令
     * @return 解析后的回调数据
     */
    WeChatPartnerPayNotifyParsed parsePayNotify(WeChatPayNotifyParseCommand command);

    /**
     * 查询服务商订单。
     *
     * @param command 查询命令
     * @return 查询结果
     */
    WeChatPartnerOrderQueryResult queryPartnerOrder(WeChatPartnerOrderQueryCommand command);

    /**
     * 关闭服务商订单。
     *
     * @param command 关单命令
     */
    void closePartnerOrder(WeChatPartnerOrderCloseCommand command);

    /**
     * 服务商退款。
     *
     * @param command 退款命令
     * @return 退款结果
     */
    WeChatPartnerRefundResult refund(WeChatPartnerRefundCommand command);

    /**
     * 解析微信支付 V3 服务商模式退款回调通知。
     *
     * @param command 回调解析命令
     * @return 解析后的退款回调数据
     */
    WeChatPartnerRefundNotifyParsed parseRefundNotify(WeChatPayNotifyParseCommand command);
}

