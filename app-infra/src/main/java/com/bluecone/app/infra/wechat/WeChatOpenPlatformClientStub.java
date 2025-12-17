package com.bluecone.app.infra.wechat;

import org.springframework.stereotype.Component;

/**
 * 微信开放平台网关的占位实现（快速注册相关）。
 *
 * 先保证编译通过，后续再接入真实 HTTP 调用（可选引入 WxJava 或基于 WebClient/RestTemplate 自行封装）。
 * 
 * 说明：为避免与 {@link com.bluecone.app.infra.wechat.openplatform.WeChatOpenPlatformClientStub} 的 Bean 名称冲突，
 * 此处指定 Bean 名称为 weChatFastRegisterClientStub。
 */
@Component("weChatFastRegisterClientStub")
public class WeChatOpenPlatformClientStub implements WeChatOpenPlatformClient {

    @Override
    public WeChatBetaRegisterResult fastRegisterBetaWeapp(WeChatBetaRegisterRequest request) {
        throw new UnsupportedOperationException("TODO: integrate with WeChat fastregisterbetaweapp");
    }

    @Override
    public WeChatFastRegisterResult fastRegisterWeapp(WeChatFastRegisterRequest request) {
        throw new UnsupportedOperationException("TODO: integrate with WeChat fastregisterweapp");
    }

    @Override
    public WeChatRegisterStatusResult queryRegisterStatus(WeChatRegisterStatusQuery query) {
        throw new UnsupportedOperationException("TODO: integrate with WeChat fastregisterweapp.search or related API");
    }
}

