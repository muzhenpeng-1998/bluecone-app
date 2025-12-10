package com.bluecone.app.api.onboarding.dto;

/**
 * 创建入驻会话请求。
 */
public class StartSessionRequest {

    /**
     * 渠道代码，例如 coffee-2025、douyin-ad-01，可为空。
     */
    private String channelCode;

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }
}

