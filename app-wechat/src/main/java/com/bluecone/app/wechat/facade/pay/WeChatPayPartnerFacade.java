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
}

