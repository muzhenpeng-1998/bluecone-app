package com.bluecone.app.infra.wechat;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 微信快速注册接口的占位实现。
 *
 * 用于本地开发/测试时的占位实现，不真正调用微信接口。
 * 后续会在同包下新增基于 WebClient 或 RestTemplate 的正式实现。
 */
@Component
@Profile({"local", "dev", "stub-wechat"})
public class WeChatFastRegisterClientStub implements WeChatFastRegisterClient {

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
