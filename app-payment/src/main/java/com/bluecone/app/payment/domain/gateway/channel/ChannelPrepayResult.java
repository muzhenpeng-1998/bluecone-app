package com.bluecone.app.payment.domain.gateway.channel;

import java.util.Map;
import java.util.Objects;

/**
 * 通用渠道预下单结果：承载第三方预支付单号及前端唤起参数。
 */
public class ChannelPrepayResult {

    private final String channelPrepayId;
    private final Map<String, Object> channelContext;

    public ChannelPrepayResult(final String channelPrepayId,
                               final Map<String, Object> channelContext) {
        this.channelPrepayId = channelPrepayId;
        this.channelContext = Objects.requireNonNull(channelContext, "channelContext must not be null");
    }

    public String getChannelPrepayId() {
        return channelPrepayId;
    }

    public Map<String, Object> getChannelContext() {
        return channelContext;
    }
}
